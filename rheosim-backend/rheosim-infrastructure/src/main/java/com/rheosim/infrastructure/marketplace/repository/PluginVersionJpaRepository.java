package com.rheosim.infrastructure.marketplace.repository;

import com.rheosim.infrastructure.marketplace.entity.PluginVersionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginVersionJpaRepository extends JpaRepository<PluginVersionJpaEntity, UUID> {

    List<PluginVersionJpaEntity> findByPluginIdOrderByCreatedAtDesc(UUID pluginId);

    Optional<PluginVersionJpaEntity> findFirstByPluginIdAndStatusOrderByCreatedAtDesc(UUID pluginId, String status);
}
