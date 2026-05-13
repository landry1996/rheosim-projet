package com.rheosim.infrastructure.marketplace.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plugin_reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"plugin_id", "user_id"}))
public class PluginReviewJpaEntity {

    @Id
    private UUID id;

    @Column(name = "plugin_id", nullable = false)
    private UUID pluginId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private Instant createdAt;

    public PluginReviewJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPluginId() { return pluginId; }
    public void setPluginId(UUID pluginId) { this.pluginId = pluginId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
