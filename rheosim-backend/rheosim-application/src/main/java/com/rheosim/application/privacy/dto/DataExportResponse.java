package com.rheosim.application.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record DataExportResponse(
        UUID userId,
        String downloadUrl,
        Instant expiresAt,
        long sizeBytes
) {}
