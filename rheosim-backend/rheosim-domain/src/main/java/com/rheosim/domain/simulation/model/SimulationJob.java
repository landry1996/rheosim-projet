package com.rheosim.domain.simulation.model;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.shared.BaseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class SimulationJob extends BaseEntity {

    private UUID projectId;
    private UUID datasetId;
    private UUID materialId;
    private UUID submittedBy;
    private SimulationType simulationType;
    private ConstitutiveModelType modelType;
    private int numberOfBranches;
    private JobStatus status;
    private double progress;
    private String resultData;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    private SimulationJob() {
        this.status = JobStatus.QUEUED;
        this.progress = 0.0;
    }

    public void start() {
        if (this.status != JobStatus.QUEUED) {
            throw new IllegalStateException("Job must be QUEUED to start, current: " + status);
        }
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
        setUpdatedAt(Instant.now());
    }

    public void updateProgress(double progress) {
        if (this.status != JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot update progress of non-running job");
        }
        this.progress = Math.min(100.0, Math.max(0.0, progress));
        setUpdatedAt(Instant.now());
    }

    public void complete(String resultData) {
        if (this.status != JobStatus.RUNNING) {
            throw new IllegalStateException("Job must be RUNNING to complete");
        }
        this.status = JobStatus.COMPLETED;
        this.progress = 100.0;
        this.resultData = resultData;
        this.completedAt = Instant.now();
        setUpdatedAt(Instant.now());
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        setUpdatedAt(Instant.now());
    }

    public void cancel() {
        if (this.status == JobStatus.COMPLETED || this.status == JobStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a finished job");
        }
        this.status = JobStatus.CANCELLED;
        this.completedAt = Instant.now();
        setUpdatedAt(Instant.now());
    }

    public Duration getElapsedTime() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    // Getters
    public UUID getProjectId() { return projectId; }
    public UUID getDatasetId() { return datasetId; }
    public UUID getMaterialId() { return materialId; }
    public UUID getSubmittedBy() { return submittedBy; }
    public SimulationType getSimulationType() { return simulationType; }
    public ConstitutiveModelType getModelType() { return modelType; }
    public int getNumberOfBranches() { return numberOfBranches; }
    public JobStatus getStatus() { return status; }
    public double getProgress() { return progress; }
    public String getResultData() { return resultData; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    // Setters for adapter reconstruction
    @Override
    public void setId(UUID id) { super.setId(id); }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public void setMaterialId(UUID materialId) { this.materialId = materialId; }
    public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }
    public void setSimulationType(SimulationType simulationType) { this.simulationType = simulationType; }
    public void setModelType(ConstitutiveModelType modelType) { this.modelType = modelType; }
    public void setNumberOfBranches(int numberOfBranches) { this.numberOfBranches = numberOfBranches; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setProgress(double progress) { this.progress = progress; }
    public void setResultData(String resultData) { this.resultData = resultData; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SimulationJob job = new SimulationJob();

        public Builder projectId(UUID projectId) { job.projectId = projectId; return this; }
        public Builder datasetId(UUID datasetId) { job.datasetId = datasetId; return this; }
        public Builder materialId(UUID materialId) { job.materialId = materialId; return this; }
        public Builder submittedBy(UUID submittedBy) { job.submittedBy = submittedBy; return this; }
        public Builder simulationType(SimulationType type) { job.simulationType = type; return this; }
        public Builder modelType(ConstitutiveModelType modelType) { job.modelType = modelType; return this; }
        public Builder numberOfBranches(int n) { job.numberOfBranches = n; return this; }

        public SimulationJob build() {
            job.setId(UUID.randomUUID());
            job.setCreatedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            return job;
        }
    }
}
