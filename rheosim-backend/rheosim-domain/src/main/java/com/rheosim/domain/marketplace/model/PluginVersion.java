package com.rheosim.domain.marketplace.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class PluginVersion extends BaseEntity {
    private UUID pluginId;
    private String version;
    private String artifactUrl;
    private String artifactHash;
    private VersionStatus status;
    private String changelog;
    private Instant createdAt;

    public PluginVersion(UUID id, UUID pluginId, String version,
                         String artifactUrl, String artifactHash, String changelog) {
        super(id);
        this.pluginId = pluginId;
        this.version = version;
        this.artifactUrl = artifactUrl;
        this.artifactHash = artifactHash;
        this.status = VersionStatus.PENDING;
        this.changelog = changelog;
        this.createdAt = Instant.now();
    }

    public void approve() { this.status = VersionStatus.APPROVED; }
    public void reject() { this.status = VersionStatus.REJECTED; }

    public UUID getPluginId() { return pluginId; }
    public String getVersion() { return version; }
    public String getArtifactUrl() { return artifactUrl; }
    public String getArtifactHash() { return artifactHash; }
    public VersionStatus getStatus() { return status; }
    public String getChangelog() { return changelog; }
    public Instant getCreatedAt() { return createdAt; }
}
