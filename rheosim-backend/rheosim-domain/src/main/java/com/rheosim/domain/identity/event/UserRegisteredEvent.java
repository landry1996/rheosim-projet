package com.rheosim.domain.identity.event;

import com.rheosim.domain.shared.DomainEvent;

import java.util.UUID;

public class UserRegisteredEvent extends DomainEvent {

    private final UUID userId;
    private final String email;
    private final String fullName;

    public UserRegisteredEvent(UUID userId, String email, String fullName) {
        super();
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }

    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}
