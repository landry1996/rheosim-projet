package com.rheosim.application.experiment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetResponse(
        UUID id,
        String fileName,
        String originalFileName,
        long fileSize,
        String contentType,
        UUID projectId,
        UUID uploadedBy,
        String experimentType,
        String status,
        int rowCount,
        List<DataColumnResponse> columns,
        double temperature,
        String temperatureUnit,
        String notes,
        List<ValidationErrorResponse> validationErrors,
        Instant createdAt,
        Instant updatedAt
) {}
