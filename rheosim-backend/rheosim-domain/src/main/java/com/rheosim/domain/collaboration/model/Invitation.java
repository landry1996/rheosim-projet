package com.rheosim.domain.collaboration.model;

import java.time.Instant;
import java.util.UUID;

public record Invitation(
        UUID id,
        UUID organizationId,
        String email,
        OrganizationRole role,
        InvitationStatus status,
        UUID invitedByUserId,
        Instant createdAt,
        Instant expiresAt
) {
    public static Invitation create(UUID organizationId, String email, OrganizationRole role, UUID invitedBy) {
        return new Invitation(
                UUID.randomUUID(),
                organizationId,
                email,
                role,
                InvitationStatus.PENDING,
                invitedBy,
                Instant.now(),
                Instant.now().plusSeconds(7 * 24 * 3600)
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
