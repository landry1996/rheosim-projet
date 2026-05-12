package com.rheosim.domain.shared;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID userId,
        String action,
        String entity,
        UUID entityId,
        Map<String, Object> details,
        String ipAddress,
        Instant timestamp
) {
    public AuditEvent(UUID userId, String action, String entity, UUID entityId, String ipAddress) {
        this(userId, action, entity, entityId, Map.of(), ipAddress, Instant.now());
    }
}
