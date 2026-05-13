package com.rheosim.application.marketplace.dto;

import java.util.List;

public record PluginSearchRequest(
        String query,
        List<String> tags,
        int page,
        int size
) {
    public PluginSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 20;
    }
}
