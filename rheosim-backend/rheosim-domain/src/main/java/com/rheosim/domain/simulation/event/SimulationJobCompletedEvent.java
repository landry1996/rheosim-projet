package com.rheosim.domain.simulation.event;

import com.rheosim.domain.shared.DomainEvent;

import java.util.UUID;

public class SimulationJobCompletedEvent extends DomainEvent {

    private final UUID jobId;
    private final UUID projectId;
    private final boolean success;
    private final long elapsedMillis;

    public SimulationJobCompletedEvent(UUID jobId, UUID projectId, boolean success, long elapsedMillis) {
        super();
        this.jobId = jobId;
        this.projectId = projectId;
        this.success = success;
        this.elapsedMillis = elapsedMillis;
    }

    public UUID getJobId() { return jobId; }
    public UUID getProjectId() { return projectId; }
    public boolean isSuccess() { return success; }
    public long getElapsedMillis() { return elapsedMillis; }

    @Override
    public String getEventType() { return "SIMULATION_JOB_COMPLETED"; }
}
