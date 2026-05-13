package com.rheosim.infrastructure.marketplace.repository;

import com.rheosim.infrastructure.marketplace.entity.PluginReviewJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PluginReviewJpaRepository extends JpaRepository<PluginReviewJpaEntity, UUID> {

    Page<PluginReviewJpaEntity> findByPluginIdOrderByCreatedAtDesc(UUID pluginId, Pageable pageable);

    Optional<PluginReviewJpaEntity> findByPluginIdAndUserId(UUID pluginId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM PluginReviewJpaEntity r WHERE r.pluginId = :pluginId")
    Double averageRatingByPluginId(@Param("pluginId") UUID pluginId);

    int countByPluginId(UUID pluginId);
}
