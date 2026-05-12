package com.rheosim.domain.experiment.event;

import com.rheosim.domain.shared.DomainEvent;

import java.util.UUID;

public class DatasetUploadedEvent extends DomainEvent {

    private final UUID datasetId;
    private final UUID projectId;
    private final UUID uploadedBy;
    private final String fileName;

    public DatasetUploadedEvent(UUID datasetId, UUID projectId, UUID uploadedBy, String fileName) {
        super();
        this.datasetId = datasetId;
        this.projectId = projectId;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
    }

    public UUID getDatasetId() { return datasetId; }
    public UUID getProjectId() { return projectId; }
    public UUID getUploadedBy() { return uploadedBy; }
    public String getFileName() { return fileName; }

    @Override
    public String getEventType() { return "DATASET_UPLOADED"; }
}
