package com.rheosim.domain.marketplace.port;

import com.rheosim.domain.marketplace.model.PluginReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginReviewRepository {
    PluginReview save(PluginReview review);
    List<PluginReview> findByPluginId(UUID pluginId, int page, int size);
    Optional<PluginReview> findByPluginIdAndUserId(UUID pluginId, UUID userId);
    double averageRatingByPluginId(UUID pluginId);
    int countByPluginId(UUID pluginId);
}
