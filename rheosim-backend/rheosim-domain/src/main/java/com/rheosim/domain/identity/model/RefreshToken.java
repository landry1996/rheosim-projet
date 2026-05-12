package com.rheosim.domain.identity.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken extends BaseEntity {

    private String token;
    private UUID userId;
    private Instant expiresAt;
    private boolean revoked;

    private RefreshToken() {
        super();
    }

    public RefreshToken(UUID userId, String token, Instant expiresAt) {
        super();
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
        markUpdated();
    }

    public String getToken() { return token; }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }

    public void setId(UUID id) { super.setId(id); }
    public void setToken(String token) { this.token = token; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
