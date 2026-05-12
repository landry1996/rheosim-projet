package com.rheosim.domain.collaboration.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID organizationId,
        UUID userId,
        String action,
        String resourceType,
        UUID resourceId,
        String details,
        Instant occurredAt
) {
    public static AuditEvent create(UUID orgId, UUID userId, String action, String resourceType, UUID resourceId, String details) {
        return new AuditEvent(UUID.randomUUID(), orgId, userId, action, resourceType, resourceId, details, Instant.now());
    }
}
