package com.rheosim.application.identity.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String organization,
        Set<String> roles,
        Instant createdAt
) {}
