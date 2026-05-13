package com.rheosim.application.marketplace.usecase;

import com.rheosim.application.marketplace.dto.*;
import com.rheosim.domain.marketplace.model.*;
import com.rheosim.domain.marketplace.port.PluginRepository;
import com.rheosim.domain.marketplace.port.PluginReviewRepository;
import com.rheosim.domain.marketplace.port.PluginVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MarketplaceUseCase {

    private final PluginRepository pluginRepository;
    private final PluginVersionRepository versionRepository;
    private final PluginReviewRepository reviewRepository;

    public MarketplaceUseCase(PluginRepository pluginRepository,
                              PluginVersionRepository versionRepository,
                              PluginReviewRepository reviewRepository) {
        this.pluginRepository = pluginRepository;
        this.versionRepository = versionRepository;
        this.reviewRepository = reviewRepository;
    }

    public Plugin createPlugin(CreatePluginRequest request, UUID authorId) {
        String slug = generateSlug(request.name());
        Plugin plugin = new Plugin(
                UUID.randomUUID(),
                request.name(),
                slug,
                request.description(),
                authorId,
                request.license(),
                request.tags()
        );
        return pluginRepository.save(plugin);
    }

    @Transactional(readOnly = true)
    public Plugin getPlugin(UUID pluginId) {
        return pluginRepository.findById(pluginId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginId));
    }

    @Transactional(readOnly = true)
    public Plugin getPluginBySlug(String slug) {
        return pluginRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<Plugin> listPlugins(int page, int size) {
        return pluginRepository.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public List<Plugin> searchPlugins(PluginSearchRequest request) {
        return pluginRepository.search(request.query(), request.tags(), request.page(), request.size());
    }

    public PluginVersion publishVersion(UUID pluginId, CreateVersionRequest request, UUID authorId) {
        Plugin plugin = getPlugin(pluginId);
        if (!plugin.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("Only the plugin author can publish versions");
        }

        PluginVersion version = new PluginVersion(
                UUID.randomUUID(),
                pluginId,
                request.version(),
                request.artifactUrl(),
                request.artifactHash(),
                request.changelog()
        );
        return versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<PluginVersion> getVersions(UUID pluginId) {
        return versionRepository.findByPluginId(pluginId);
    }

    public PluginReview addReview(UUID pluginId, CreateReviewRequest request, UUID userId) {
        reviewRepository.findByPluginIdAndUserId(pluginId, userId).ifPresent(existing -> {
            throw new IllegalStateException("User already reviewed this plugin");
        });

        PluginReview review = new PluginReview(
                UUID.randomUUID(),
                pluginId,
                userId,
                request.rating(),
                request.comment()
        );
        PluginReview saved = reviewRepository.save(review);

        Plugin plugin = getPlugin(pluginId);
        plugin.addRating(request.rating());
        pluginRepository.save(plugin);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PluginReview> getReviews(UUID pluginId, int page, int size) {
        return reviewRepository.findByPluginId(pluginId, page, size);
    }

    public void downloadPlugin(UUID pluginId) {
        Plugin plugin = getPlugin(pluginId);
        plugin.incrementDownloads();
        pluginRepository.save(plugin);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
