package com.rheosim.infrastructure.search.dto;

import java.util.List;
import java.util.Map;

public record SearchResult(
        List<SearchHit> hits,
        long totalHits,
        int page,
        int size,
        Map<String, Long> facets
) {
    public record SearchHit(
            String id,
            String index,
            Double score,
            Map source,
            Map<String, List<String>> highlights
    ) {
        public String getType() {
            if (index == null) return "unknown";
            return switch (index) {
                case "rheosim-plugins" -> "plugin";
                case "rheosim-projects" -> "project";
                case "rheosim-materials" -> "material";
                default -> "unknown";
            };
        }
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalHits / size);
    }

    public boolean hasNext() {
        return page < totalPages() - 1;
    }
}
