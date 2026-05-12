package com.rheosim.application.experiment.usecase;

import com.rheosim.application.experiment.dto.*;
import com.rheosim.domain.experiment.model.*;
import com.rheosim.domain.experiment.port.DataParserPort;
import com.rheosim.domain.experiment.port.DataValidatorPort;
import com.rheosim.domain.experiment.port.DatasetRepository;
import com.rheosim.domain.experiment.port.FileStoragePort;
import com.rheosim.domain.project.port.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DatasetUseCase {

    private final DatasetRepository datasetRepository;
    private final ProjectRepository projectRepository;
    private final FileStoragePort fileStoragePort;
    private final List<DataParserPort> parsers;
    private final DataValidatorPort validator;

    public DatasetUseCase(DatasetRepository datasetRepository,
                          ProjectRepository projectRepository,
                          FileStoragePort fileStoragePort,
                          List<DataParserPort> parsers,
                          DataValidatorPort validator) {
        this.datasetRepository = datasetRepository;
        this.projectRepository = projectRepository;
        this.fileStoragePort = fileStoragePort;
        this.parsers = parsers;
        this.validator = validator;
    }

    public DatasetResponse uploadDataset(UploadDatasetRequest request,
                                         String originalFileName,
                                         String contentType,
                                         long fileSize,
                                         InputStream fileContent,
                                         UUID uploadedBy) {
        projectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));

        ExperimentType experimentType = ExperimentType.valueOf(request.experimentType().toUpperCase());

        Dataset dataset = Dataset.builder()
                .originalFileName(originalFileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .projectId(request.projectId())
                .uploadedBy(uploadedBy)
                .experimentType(experimentType)
                .temperature(request.temperature() != null ? request.temperature() : 25.0)
                .temperatureUnit(request.temperatureUnit() != null ? request.temperatureUnit() : "°C")
                .notes(request.notes())
                .build();

        String storagePath = fileStoragePort.store(dataset.getId(), originalFileName, fileContent);
        dataset.setFileName(storagePath);

        Dataset saved = datasetRepository.save(dataset);
        return toResponse(saved);
    }

    public DatasetResponse validateDataset(UUID datasetId, UUID userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        dataset.markValidating();

        String contentType = dataset.getContentType();
        InputStream fileContent = fileStoragePort.load(dataset.getFileName());

        DataParserPort parser = parsers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No parser available for content type: " + contentType));

        DataParserPort.ParseResult parseResult = parser.parse(fileContent, contentType);

        List<ValidationError> errors = validator.validate(
                dataset.getExperimentType(),
                parseResult.columns(),
                parseResult.rows()
        );

        boolean hasErrors = errors.stream()
                .anyMatch(e -> e.severity() == ValidationError.Severity.ERROR);

        if (hasErrors) {
            dataset.markInvalid(errors);
        } else {
            dataset.markValid(parseResult.totalRows(), parseResult.columns());
            if (!errors.isEmpty()) {
                dataset.setValidationErrors(errors);
            }
        }

        Dataset saved = datasetRepository.save(dataset);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DatasetResponse getDataset(UUID datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        return toResponse(dataset);
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> listDatasetsByProject(UUID projectId) {
        return datasetRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> listValidDatasets(UUID projectId) {
        return datasetRepository.findByProjectIdAndStatus(projectId, DatasetStatus.VALID).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteDataset(UUID datasetId, UUID userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        fileStoragePort.delete(dataset.getFileName());
        datasetRepository.deleteById(datasetId);
    }

    private DatasetResponse toResponse(Dataset dataset) {
        List<DataColumnResponse> columnResponses = dataset.getColumns().stream()
                .map(c -> new DataColumnResponse(c.name(), c.unit(), c.quantity().name()))
                .toList();

        List<ValidationErrorResponse> errorResponses = dataset.getValidationErrors().stream()
                .map(e -> new ValidationErrorResponse(e.row(), e.column(), e.message(), e.severity().name()))
                .toList();

        return new DatasetResponse(
                dataset.getId(),
                dataset.getFileName(),
                dataset.getOriginalFileName(),
                dataset.getFileSize(),
                dataset.getContentType(),
                dataset.getProjectId(),
                dataset.getUploadedBy(),
                dataset.getExperimentType().name(),
                dataset.getStatus().name(),
                dataset.getRowCount(),
                columnResponses,
                dataset.getTemperature(),
                dataset.getTemperatureUnit(),
                dataset.getNotes(),
                errorResponses,
                dataset.getCreatedAt(),
                dataset.getUpdatedAt()
        );
    }
}
