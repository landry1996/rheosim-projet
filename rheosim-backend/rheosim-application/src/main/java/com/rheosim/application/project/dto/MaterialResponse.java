package com.rheosim.application.project.dto;

import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String name,
        String grade,
        String family,
        String supplier,
        String notes,
        UUID projectId,
        int version,
        MaterialModelResponse model,
        Instant createdAt,
        Instant updatedAt
) {}
