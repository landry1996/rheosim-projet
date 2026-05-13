package com.rheosim.application.marketplace.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PluginResponse(
        UUID id,
        String name,
        String slug,
        String description,
        UUID authorId,
        String authorName,
        String license,
        List<String> tags,
        int downloadsCount,
        double ratingAverage,
        int ratingCount,
        String status,
        String latestVersion,
        Instant createdAt
) {}
