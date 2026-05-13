package com.rheosim.domain.marketplace.port;

import com.rheosim.domain.marketplace.model.PluginVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginVersionRepository {
    PluginVersion save(PluginVersion version);
    Optional<PluginVersion> findById(UUID id);
    List<PluginVersion> findByPluginId(UUID pluginId);
    Optional<PluginVersion> findLatestApproved(UUID pluginId);
    void delete(UUID id);
}
