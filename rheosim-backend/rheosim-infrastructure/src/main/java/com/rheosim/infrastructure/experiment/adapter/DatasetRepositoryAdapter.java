package com.rheosim.infrastructure.experiment.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rheosim.domain.experiment.model.*;
import com.rheosim.domain.experiment.port.DatasetRepository;
import com.rheosim.infrastructure.experiment.entity.DatasetJpaEntity;
import com.rheosim.infrastructure.experiment.repository.DatasetJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DatasetRepositoryAdapter implements DatasetRepository {

    private final DatasetJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public DatasetRepositoryAdapter(DatasetJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Dataset save(Dataset dataset) {
        DatasetJpaEntity entity = toJpaEntity(dataset);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Dataset> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Dataset> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Dataset> findByProjectIdAndStatus(UUID projectId, DatasetStatus status) {
        return jpaRepository.findByProjectIdAndStatus(projectId, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private DatasetJpaEntity toJpaEntity(Dataset dataset) {
        DatasetJpaEntity entity = new DatasetJpaEntity();
        entity.setId(dataset.getId());
        entity.setFileName(dataset.getFileName());
        entity.setOriginalFileName(dataset.getOriginalFileName());
        entity.setFileSize(dataset.getFileSize());
        entity.setContentType(dataset.getContentType());
        entity.setProjectId(dataset.getProjectId());
        entity.setUploadedBy(dataset.getUploadedBy());
        entity.setExperimentType(dataset.getExperimentType().name());
        entity.setStatus(dataset.getStatus().name());
        entity.setRowCount(dataset.getRowCount());
        entity.setTemperature(dataset.getTemperature());
        entity.setTemperatureUnit(dataset.getTemperatureUnit());
        entity.setNotes(dataset.getNotes());
        entity.setCreatedAt(dataset.getCreatedAt());
        entity.setUpdatedAt(dataset.getUpdatedAt());

        try {
            if (!dataset.getColumns().isEmpty()) {
                entity.setColumnsMetadata(objectMapper.writeValueAsString(dataset.getColumns()));
            }
            if (!dataset.getValidationErrors().isEmpty()) {
                entity.setValidationErrors(objectMapper.writeValueAsString(dataset.getValidationErrors()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dataset metadata", e);
        }

        return entity;
    }

    private Dataset toDomain(DatasetJpaEntity entity) {
        Dataset dataset = Dataset.builder()
                .originalFileName(entity.getOriginalFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .projectId(entity.getProjectId())
                .uploadedBy(entity.getUploadedBy())
                .experimentType(ExperimentType.valueOf(entity.getExperimentType()))
                .temperature(entity.getTemperature())
                .temperatureUnit(entity.getTemperatureUnit())
                .notes(entity.getNotes())
                .build();

        dataset.setId(entity.getId());
        dataset.setFileName(entity.getFileName());
        dataset.setStatus(DatasetStatus.valueOf(entity.getStatus()));
        dataset.setRowCount(entity.getRowCount());
        dataset.setCreatedAt(entity.getCreatedAt());
        dataset.setUpdatedAt(entity.getUpdatedAt());

        try {
            if (entity.getColumnsMetadata() != null && !entity.getColumnsMetadata().isBlank()) {
                List<DataColumn> columns = objectMapper.readValue(
                        entity.getColumnsMetadata(), new TypeReference<>() {});
                dataset.setColumns(columns);
            }
            if (entity.getValidationErrors() != null && !entity.getValidationErrors().isBlank()) {
                List<ValidationError> errors = objectMapper.readValue(
                        entity.getValidationErrors(), new TypeReference<>() {});
                dataset.setValidationErrors(errors);
            }
        } catch (JsonProcessingException e) {
            dataset.setColumns(Collections.emptyList());
            dataset.setValidationErrors(Collections.emptyList());
        }

        return dataset;
    }
}
