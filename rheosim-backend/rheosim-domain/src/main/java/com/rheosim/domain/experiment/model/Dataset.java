package com.rheosim.domain.experiment.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Dataset extends BaseEntity {

    private String fileName;
    private String originalFileName;
    private long fileSize;
    private String contentType;
    private UUID projectId;
    private UUID uploadedBy;
    private ExperimentType experimentType;
    private DatasetStatus status;
    private List<DataColumn> columns;
    private int rowCount;
    private double temperature;
    private String temperatureUnit;
    private String notes;
    private List<ValidationError> validationErrors;

    private Dataset() {
        this.columns = new ArrayList<>();
        this.validationErrors = new ArrayList<>();
        this.status = DatasetStatus.UPLOADED;
    }

    public void markValidating() {
        this.status = DatasetStatus.VALIDATING;
        this.setUpdatedAt(Instant.now());
    }

    public void markValid(int rowCount, List<DataColumn> columns) {
        this.status = DatasetStatus.VALID;
        this.rowCount = rowCount;
        this.columns = new ArrayList<>(columns);
        this.validationErrors.clear();
        this.setUpdatedAt(Instant.now());
    }

    public void markInvalid(List<ValidationError> errors) {
        this.status = DatasetStatus.INVALID;
        this.validationErrors = new ArrayList<>(errors);
        this.setUpdatedAt(Instant.now());
    }

    public void markProcessing() {
        if (this.status != DatasetStatus.VALID) {
            throw new IllegalStateException("Dataset must be VALID before processing");
        }
        this.status = DatasetStatus.PROCESSING;
        this.setUpdatedAt(Instant.now());
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public long getFileSize() { return fileSize; }
    public String getContentType() { return contentType; }
    public UUID getProjectId() { return projectId; }
    public UUID getUploadedBy() { return uploadedBy; }
    public ExperimentType getExperimentType() { return experimentType; }
    public DatasetStatus getStatus() { return status; }
    public List<DataColumn> getColumns() { return Collections.unmodifiableList(columns); }
    public int getRowCount() { return rowCount; }
    public double getTemperature() { return temperature; }
    public String getTemperatureUnit() { return temperatureUnit; }
    public String getNotes() { return notes; }
    public List<ValidationError> getValidationErrors() { return Collections.unmodifiableList(validationErrors); }

    // Setters for adapter reconstruction
    @Override
    public void setId(UUID id) { super.setId(id); }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setExperimentType(ExperimentType experimentType) { this.experimentType = experimentType; }
    public void setStatus(DatasetStatus status) { this.status = status; }
    public void setColumns(List<DataColumn> columns) { this.columns = new ArrayList<>(columns); }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setTemperatureUnit(String temperatureUnit) { this.temperatureUnit = temperatureUnit; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setValidationErrors(List<ValidationError> errors) { this.validationErrors = new ArrayList<>(errors); }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Dataset dataset = new Dataset();

        public Builder fileName(String fileName) { dataset.fileName = fileName; return this; }
        public Builder originalFileName(String originalFileName) { dataset.originalFileName = originalFileName; return this; }
        public Builder fileSize(long fileSize) { dataset.fileSize = fileSize; return this; }
        public Builder contentType(String contentType) { dataset.contentType = contentType; return this; }
        public Builder projectId(UUID projectId) { dataset.projectId = projectId; return this; }
        public Builder uploadedBy(UUID uploadedBy) { dataset.uploadedBy = uploadedBy; return this; }
        public Builder experimentType(ExperimentType experimentType) { dataset.experimentType = experimentType; return this; }
        public Builder temperature(double temperature) { dataset.temperature = temperature; return this; }
        public Builder temperatureUnit(String temperatureUnit) { dataset.temperatureUnit = temperatureUnit; return this; }
        public Builder notes(String notes) { dataset.notes = notes; return this; }

        public Dataset build() {
            dataset.setId(UUID.randomUUID());
            dataset.setCreatedAt(Instant.now());
            dataset.setUpdatedAt(Instant.now());
            return dataset;
        }
    }
}
