package com.rheosim.application.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String status,
        UUID ownerId,
        List<UUID> materialIds,
        Instant createdAt,
        Instant updatedAt
) {}
