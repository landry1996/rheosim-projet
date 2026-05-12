package com.rheosim.domain.reporting.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class Report extends BaseEntity {

    private UUID projectId;
    private UUID jobId;
    private UUID generatedBy;
    private String title;
    private ReportFormat format;
    private ReportStatus status;
    private String filePath;
    private long fileSize;
    private DublinCoreMetadata metadata;
    private String errorMessage;

    private Report() {
        this.status = ReportStatus.GENERATING;
    }

    public void markReady(String filePath, long fileSize) {
        this.status = ReportStatus.READY;
        this.filePath = filePath;
        this.fileSize = fileSize;
        setUpdatedAt(Instant.now());
    }

    public void markFailed(String errorMessage) {
        this.status = ReportStatus.FAILED;
        this.errorMessage = errorMessage;
        setUpdatedAt(Instant.now());
    }

    public void markExpired() {
        this.status = ReportStatus.EXPIRED;
        setUpdatedAt(Instant.now());
    }

    // Getters
    public UUID getProjectId() { return projectId; }
    public UUID getJobId() { return jobId; }
    public UUID getGeneratedBy() { return generatedBy; }
    public String getTitle() { return title; }
    public ReportFormat getFormat() { return format; }
    public ReportStatus getStatus() { return status; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
    public DublinCoreMetadata getMetadata() { return metadata; }
    public String getErrorMessage() { return errorMessage; }

    // Setters for adapter reconstruction
    @Override
    public void setId(UUID id) { super.setId(id); }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public void setGeneratedBy(UUID generatedBy) { this.generatedBy = generatedBy; }
    public void setTitle(String title) { this.title = title; }
    public void setFormat(ReportFormat format) { this.format = format; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setMetadata(DublinCoreMetadata metadata) { this.metadata = metadata; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Report report = new Report();

        public Builder projectId(UUID projectId) { report.projectId = projectId; return this; }
        public Builder jobId(UUID jobId) { report.jobId = jobId; return this; }
        public Builder generatedBy(UUID generatedBy) { report.generatedBy = generatedBy; return this; }
        public Builder title(String title) { report.title = title; return this; }
        public Builder format(ReportFormat format) { report.format = format; return this; }
        public Builder metadata(DublinCoreMetadata metadata) { report.metadata = metadata; return this; }

        public Report build() {
            report.setId(UUID.randomUUID());
            report.setCreatedAt(Instant.now());
            report.setUpdatedAt(Instant.now());
            return report;
        }
    }
}
