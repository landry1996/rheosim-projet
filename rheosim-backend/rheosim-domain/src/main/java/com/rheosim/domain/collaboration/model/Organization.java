package com.rheosim.domain.collaboration.model;

import java.time.Instant;
import java.util.UUID;

public record Organization(
        UUID id,
        String name,
        String slug,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static Organization create(String name, String slug, UUID ownerUserId) {
        var now = Instant.now();
        return new Organization(UUID.randomUUID(), name, slug, ownerUserId, now, now);
    }
}
