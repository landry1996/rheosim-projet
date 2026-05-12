package com.rheosim.domain.experiment.event;

import com.rheosim.domain.shared.DomainEvent;

import java.util.UUID;

public class DatasetValidatedEvent extends DomainEvent {

    private final UUID datasetId;
    private final UUID projectId;
    private final boolean valid;
    private final int errorCount;

    public DatasetValidatedEvent(UUID datasetId, UUID projectId, boolean valid, int errorCount) {
        super();
        this.datasetId = datasetId;
        this.projectId = projectId;
        this.valid = valid;
        this.errorCount = errorCount;
    }

    public UUID getDatasetId() { return datasetId; }
    public UUID getProjectId() { return projectId; }
    public boolean isValid() { return valid; }
    public int getErrorCount() { return errorCount; }

    @Override
    public String getEventType() { return "DATASET_VALIDATED"; }
}
