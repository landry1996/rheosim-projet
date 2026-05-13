package com.rheosim.domain.marketplace.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Plugin extends BaseEntity {
    private String name;
    private String slug;
    private String description;
    private UUID authorId;
    private String license;
    private List<String> tags;
    private int downloadsCount;
    private double ratingAverage;
    private int ratingCount;
    private PluginStatus status;
    private Instant createdAt;

    public Plugin(UUID id, String name, String slug, String description,
                  UUID authorId, String license, List<String> tags) {
        super(id);
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.authorId = authorId;
        this.license = license;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.downloadsCount = 0;
        this.ratingAverage = 0.0;
        this.ratingCount = 0;
        this.status = PluginStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void incrementDownloads() {
        this.downloadsCount++;
    }

    public void addRating(int rating) {
        double totalScore = this.ratingAverage * this.ratingCount + rating;
        this.ratingCount++;
        this.ratingAverage = totalScore / this.ratingCount;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public UUID getAuthorId() { return authorId; }
    public String getLicense() { return license; }
    public List<String> getTags() { return tags; }
    public int getDownloadsCount() { return downloadsCount; }
    public double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
    public PluginStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
