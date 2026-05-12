package com.rheosim.domain.simulation.event;

import com.rheosim.domain.shared.DomainEvent;

import java.util.UUID;

public class SimulationJobSubmittedEvent extends DomainEvent {

    private final UUID jobId;
    private final UUID projectId;
    private final UUID submittedBy;
    private final String modelType;

    public SimulationJobSubmittedEvent(UUID jobId, UUID projectId, UUID submittedBy, String modelType) {
        super();
        this.jobId = jobId;
        this.projectId = projectId;
        this.submittedBy = submittedBy;
        this.modelType = modelType;
    }

    public UUID getJobId() { return jobId; }
    public UUID getProjectId() { return projectId; }
    public UUID getSubmittedBy() { return submittedBy; }
    public String getModelType() { return modelType; }

    @Override
    public String getEventType() { return "SIMULATION_JOB_SUBMITTED"; }
}
