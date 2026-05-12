package com.rheosim.application.reporting.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID projectId,
        UUID jobId,
        UUID generatedBy,
        String title,
        String format,
        String status,
        String filePath,
        long fileSize,
        String errorMessage,
        Instant createdAt
) {}
