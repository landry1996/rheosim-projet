package com.rheosim.application.simulation.dto;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID projectId,
        UUID datasetId,
        UUID materialId,
        UUID submittedBy,
        String simulationType,
        String modelType,
        int numberOfBranches,
        String status,
        double progress,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {}
