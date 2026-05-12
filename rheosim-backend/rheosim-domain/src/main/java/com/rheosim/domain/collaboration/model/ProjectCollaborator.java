package com.rheosim.domain.collaboration.model;

import java.time.Instant;
import java.util.UUID;

public record ProjectCollaborator(
        UUID id,
        UUID projectId,
        UUID userId,
        ProjectRole role,
        Instant addedAt
) {
    public static ProjectCollaborator create(UUID projectId, UUID userId, ProjectRole role) {
        return new ProjectCollaborator(UUID.randomUUID(), projectId, userId, role, Instant.now());
    }
}
