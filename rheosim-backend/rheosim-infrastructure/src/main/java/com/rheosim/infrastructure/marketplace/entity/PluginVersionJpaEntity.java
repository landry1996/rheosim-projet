package com.rheosim.infrastructure.marketplace.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plugin_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"plugin_id", "version"}))
public class PluginVersionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "plugin_id", nullable = false)
    private UUID pluginId;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "artifact_url", nullable = false, length = 1000)
    private String artifactUrl;

    @Column(name = "artifact_hash", nullable = false, length = 128)
    private String artifactHash;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "created_at")
    private Instant createdAt;

    public PluginVersionJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPluginId() { return pluginId; }
    public void setPluginId(UUID pluginId) { this.pluginId = pluginId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getArtifactUrl() { return artifactUrl; }
    public void setArtifactUrl(String artifactUrl) { this.artifactUrl = artifactUrl; }
    public String getArtifactHash() { return artifactHash; }
    public void setArtifactHash(String artifactHash) { this.artifactHash = artifactHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChangelog() { return changelog; }
    public void setChangelog(String changelog) { this.changelog = changelog; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
