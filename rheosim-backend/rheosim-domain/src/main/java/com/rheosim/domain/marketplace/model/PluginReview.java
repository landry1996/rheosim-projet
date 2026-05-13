package com.rheosim.domain.marketplace.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class PluginReview extends BaseEntity {
    private UUID pluginId;
    private UUID userId;
    private int rating;
    private String comment;
    private Instant createdAt;

    public PluginReview(UUID id, UUID pluginId, UUID userId, int rating, String comment) {
        super(id);
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.pluginId = pluginId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public UUID getPluginId() { return pluginId; }
    public UUID getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
