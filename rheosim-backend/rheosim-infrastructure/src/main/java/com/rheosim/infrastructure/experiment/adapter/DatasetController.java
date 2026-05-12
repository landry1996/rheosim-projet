package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.application.experiment.dto.DatasetResponse;
import com.rheosim.application.experiment.dto.UploadDatasetRequest;
import com.rheosim.application.experiment.usecase.DatasetUseCase;
import com.rheosim.domain.identity.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/projects/{projectId}/datasets")
@Tag(name = "Datasets", description = "Experimental data import and validation")
@SecurityRequirement(name = "bearerAuth")
public class DatasetController {

    private final DatasetUseCase datasetUseCase;

    public DatasetController(DatasetUseCase datasetUseCase) {
        this.datasetUseCase = datasetUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an experimental dataset")
    public ResponseEntity<DatasetResponse> uploadDataset(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("experimentType") String experimentType,
            @RequestParam(value = "temperature", required = false) Double temperature,
            @RequestParam(value = "temperatureUnit", required = false) String temperatureUnit,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal User currentUser
    ) throws IOException {
        UploadDatasetRequest request = new UploadDatasetRequest(
                projectId, experimentType, temperature, temperatureUnit, notes
        );

        DatasetResponse response = datasetUseCase.uploadDataset(
                request,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream(),
                currentUser.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{datasetId}/validate")
    @Operation(summary = "Validate a dataset (parse and check data quality)")
    public ResponseEntity<DatasetResponse> validateDataset(
            @PathVariable UUID projectId,
            @PathVariable UUID datasetId,
            @AuthenticationPrincipal User currentUser
    ) {
        DatasetResponse response = datasetUseCase.validateDataset(datasetId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all datasets in a project")
    public ResponseEntity<List<DatasetResponse>> listDatasets(@PathVariable UUID projectId) {
        List<DatasetResponse> datasets = datasetUseCase.listDatasetsByProject(projectId);
        return ResponseEntity.ok(datasets);
    }

    @GetMapping("/valid")
    @Operation(summary = "List only validated datasets (ready for simulation)")
    public ResponseEntity<List<DatasetResponse>> listValidDatasets(@PathVariable UUID projectId) {
        List<DatasetResponse> datasets = datasetUseCase.listValidDatasets(projectId);
        return ResponseEntity.ok(datasets);
    }

    @GetMapping("/{datasetId}")
    @Operation(summary = "Get dataset details")
    public ResponseEntity<DatasetResponse> getDataset(
            @PathVariable UUID projectId,
            @PathVariable UUID datasetId
    ) {
        DatasetResponse response = datasetUseCase.getDataset(datasetId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{datasetId}")
    @Operation(summary = "Delete a dataset and its file")
    public ResponseEntity<Void> deleteDataset(
            @PathVariable UUID projectId,
            @PathVariable UUID datasetId,
            @AuthenticationPrincipal User currentUser
    ) {
        datasetUseCase.deleteDataset(datasetId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
