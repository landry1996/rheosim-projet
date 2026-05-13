package com.rheosim.domain.privacy.model;

import java.time.Instant;
import java.util.UUID;

public record UserConsent(
        UUID userId,
        ConsentType type,
        boolean granted,
        Instant grantedAt,
        Instant revokedAt,
        String ipAddress,
        String userAgent
) {
    public enum ConsentType {
        NECESSARY,
        ANALYTICS,
        MARKETING,
        FUNCTIONAL
    }

    public boolean isActive() {
        return granted && revokedAt == null;
    }

    public UserConsent revoke() {
        return new UserConsent(userId, type, false, grantedAt, Instant.now(), ipAddress, userAgent);
    }
}
