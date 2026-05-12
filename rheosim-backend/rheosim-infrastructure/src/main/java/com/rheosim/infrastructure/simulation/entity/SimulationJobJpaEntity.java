package com.rheosim.infrastructure.simulation.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_jobs", schema = "rheosim")
public class SimulationJobJpaEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "simulation_type", nullable = false, length = 30)
    private String simulationType;

    @Column(name = "model_type", nullable = false, length = 30)
    private String modelType;

    @Column(name = "number_of_branches")
    private int numberOfBranches;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "progress")
    private double progress;

    @Column(name = "result_data", columnDefinition = "JSONB")
    private String resultData;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }

    public UUID getMaterialId() { return materialId; }
    public void setMaterialId(UUID materialId) { this.materialId = materialId; }

    public UUID getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }

    public String getSimulationType() { return simulationType; }
    public void setSimulationType(String simulationType) { this.simulationType = simulationType; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    public int getNumberOfBranches() { return numberOfBranches; }
    public void setNumberOfBranches(int numberOfBranches) { this.numberOfBranches = numberOfBranches; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
