package com.rheosim.infrastructure.marketplace.adapter;

import com.rheosim.application.marketplace.dto.*;
import com.rheosim.application.marketplace.usecase.MarketplaceUseCase;
import com.rheosim.domain.marketplace.model.Plugin;
import com.rheosim.domain.marketplace.model.PluginReview;
import com.rheosim.domain.marketplace.model.PluginVersion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final MarketplaceUseCase marketplaceUseCase;

    public MarketplaceController(MarketplaceUseCase marketplaceUseCase) {
        this.marketplaceUseCase = marketplaceUseCase;
    }

    @PostMapping("/plugins")
    public ResponseEntity<PluginResponse> createPlugin(
            @Valid @RequestBody CreatePluginRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID authorId = UUID.fromString(user.getUsername());
        Plugin plugin = marketplaceUseCase.createPlugin(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(plugin));
    }

    @GetMapping("/plugins")
    public ResponseEntity<List<PluginResponse>> listPlugins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;
        List<Plugin> plugins = marketplaceUseCase.listPlugins(page, size);
        return ResponseEntity.ok(plugins.stream().map(this::toResponse).toList());
    }

    @GetMapping("/plugins/search")
    public ResponseEntity<List<PluginResponse>> searchPlugins(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var request = new PluginSearchRequest(query, tags, page, size);
        List<Plugin> plugins = marketplaceUseCase.searchPlugins(request);
        return ResponseEntity.ok(plugins.stream().map(this::toResponse).toList());
    }

    @GetMapping("/plugins/{slug}")
    public ResponseEntity<PluginResponse> getPlugin(@PathVariable String slug) {
        Plugin plugin = marketplaceUseCase.getPluginBySlug(slug);
        return ResponseEntity.ok(toResponse(plugin));
    }

    @PostMapping("/plugins/{pluginId}/versions")
    public ResponseEntity<PluginVersion> publishVersion(
            @PathVariable UUID pluginId,
            @Valid @RequestBody CreateVersionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID authorId = UUID.fromString(user.getUsername());
        PluginVersion version = marketplaceUseCase.publishVersion(pluginId, request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @GetMapping("/plugins/{pluginId}/versions")
    public ResponseEntity<List<PluginVersion>> getVersions(@PathVariable UUID pluginId) {
        return ResponseEntity.ok(marketplaceUseCase.getVersions(pluginId));
    }

    @PostMapping("/plugins/{pluginId}/reviews")
    public ResponseEntity<PluginReview> addReview(
            @PathVariable UUID pluginId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        PluginReview review = marketplaceUseCase.addReview(pluginId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/plugins/{pluginId}/reviews")
    public ResponseEntity<List<PluginReview>> getReviews(
            @PathVariable UUID pluginId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(marketplaceUseCase.getReviews(pluginId, page, size));
    }

    @PostMapping("/plugins/{pluginId}/download")
    public ResponseEntity<Void> downloadPlugin(@PathVariable UUID pluginId) {
        marketplaceUseCase.downloadPlugin(pluginId);
        return ResponseEntity.ok().build();
    }

    private PluginResponse toResponse(Plugin plugin) {
        return new PluginResponse(
                plugin.getId(),
                plugin.getName(),
                plugin.getSlug(),
                plugin.getDescription(),
                plugin.getAuthorId(),
                null,
                plugin.getLicense(),
                plugin.getTags(),
                plugin.getDownloadsCount(),
                plugin.getRatingAverage(),
                plugin.getRatingCount(),
                plugin.getStatus().name(),
                null,
                plugin.getCreatedAt()
        );
    }
}
