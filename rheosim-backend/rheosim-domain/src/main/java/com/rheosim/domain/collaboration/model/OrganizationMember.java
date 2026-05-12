package com.rheosim.domain.collaboration.model;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMember(
        UUID id,
        UUID organizationId,
        UUID userId,
        OrganizationRole role,
        Instant joinedAt
) {
    public static OrganizationMember create(UUID organizationId, UUID userId, OrganizationRole role) {
        return new OrganizationMember(UUID.randomUUID(), organizationId, userId, role, Instant.now());
    }
}
