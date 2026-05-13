package com.rheosim.infrastructure.privacy.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserConsentEntityId implements Serializable {
    private UUID userId;
    private UserConsentEntity.ConsentType type;

    public UserConsentEntityId() {}

    public UserConsentEntityId(UUID userId, UserConsentEntity.ConsentType type) {
        this.userId = userId;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConsentEntityId that = (UserConsentEntityId) o;
        return Objects.equals(userId, that.userId) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, type);
    }
}
