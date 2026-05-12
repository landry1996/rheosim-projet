package com.rheosim.domain.collaboration.port;

import com.rheosim.domain.collaboration.model.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository {
    AuditEvent save(AuditEvent event);
    List<AuditEvent> findByOrganizationId(UUID organizationId, Instant from, Instant to);
    List<AuditEvent> findByUserId(UUID userId, Instant from, Instant to);
    List<AuditEvent> findByResourceId(UUID resourceId);
}
