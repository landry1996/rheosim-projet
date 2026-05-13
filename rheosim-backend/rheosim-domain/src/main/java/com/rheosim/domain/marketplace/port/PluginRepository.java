package com.rheosim.domain.marketplace.port;

import com.rheosim.domain.marketplace.model.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginRepository {
    Plugin save(Plugin plugin);
    Optional<Plugin> findById(UUID id);
    Optional<Plugin> findBySlug(String slug);
    List<Plugin> findAll(int page, int size);
    List<Plugin> search(String query, List<String> tags, int page, int size);
    List<Plugin> findByAuthorId(UUID authorId);
    long count();
    void delete(UUID id);
}
