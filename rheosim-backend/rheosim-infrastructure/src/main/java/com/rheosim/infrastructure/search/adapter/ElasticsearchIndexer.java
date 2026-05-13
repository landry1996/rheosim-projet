package com.rheosim.infrastructure.search.adapter;

import com.rheosim.infrastructure.search.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticsearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexer.class);

    private static final String INDEX_PLUGINS = "rheosim-plugins";
    private static final String INDEX_PROJECTS = "rheosim-projects";
    private static final String INDEX_MATERIALS = "rheosim-materials";

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ElasticsearchIndexer(ElasticsearchTemplate elasticsearchTemplate, JdbcTemplate jdbcTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchResult search(String query, String type, int page, int size) {
        try {
            List<String> indices = getTargetIndices(type);
            IndexCoordinates indexCoordinates = IndexCoordinates.of(indices.toArray(new String[0]));

            Criteria criteria = new Criteria("name").matches(query)
                    .or(new Criteria("description").matches(query))
                    .or(new Criteria("tags").matches(query))
                    .or(new Criteria("family").matches(query))
                    .or(new Criteria("author").matches(query));

            CriteriaQuery searchQuery = new CriteriaQuery(criteria);
            searchQuery.setPageable(org.springframework.data.domain.PageRequest.of(page, size));

            SearchHits<Map> searchHits = elasticsearchTemplate.search(searchQuery, Map.class, indexCoordinates);

            List<SearchResult.SearchHit> hits = searchHits.getSearchHits().stream()
                    .map(hit -> new SearchResult.SearchHit(
                            hit.getId(),
                            hit.getIndex(),
                            (double) hit.getScore(),
                            hit.getContent(),
                            hit.getHighlightFields()))
                    .collect(Collectors.toList());

            long totalHits = searchHits.getTotalHits();
            Map<String, Long> facets = new HashMap<>();

            return new SearchResult(hits, totalHits, page, size, facets);
        } catch (Exception e) {
            log.error("Search failed", e);
            return new SearchResult(List.of(), 0, page, size, Map.of());
        }
    }

    public List<String> suggest(String query, int limit) {
        try {
            IndexCoordinates indexCoordinates = IndexCoordinates.of(INDEX_PLUGINS, INDEX_PROJECTS, INDEX_MATERIALS);

            Criteria criteria = new Criteria("name").startsWith(query);
            CriteriaQuery searchQuery = new CriteriaQuery(criteria);
            searchQuery.setPageable(org.springframework.data.domain.PageRequest.of(0, limit));

            SearchHits<Map> searchHits = elasticsearchTemplate.search(searchQuery, Map.class, indexCoordinates);

            return searchHits.getSearchHits().stream()
                    .map(hit -> {
                        Object name = hit.getContent().get("name");
                        return name != null ? name.toString() : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Suggest failed", e);
            return List.of();
        }
    }

    public void indexDocument(String index, String id, Map<String, Object> document) {
        try {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(id)
                    .withObject(document)
                    .build();
            elasticsearchTemplate.index(indexQuery, IndexCoordinates.of(index));
        } catch (Exception e) {
            log.error("Failed to index document {} in {}", id, index, e);
        }
    }

    public void deleteDocument(String index, String id) {
        try {
            elasticsearchTemplate.delete(id, IndexCoordinates.of(index));
        } catch (Exception e) {
            log.error("Failed to delete document {} from {}", id, index, e);
        }
    }

    public void reindexAll() {
        log.info("Starting full reindex");
        reindexProjects();
        reindexMaterials();
        log.info("Full reindex completed");
    }

    private void reindexProjects() {
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
                "SELECT id, name, description, status, created_at, updated_at FROM projects");

        for (Map<String, Object> project : projects) {
            String id = project.get("id").toString();
            indexDocument(INDEX_PROJECTS, id, project);
        }
        log.info("Reindexed {} projects", projects.size());
    }

    private void reindexMaterials() {
        List<Map<String, Object>> materials = jdbcTemplate.queryForList(
                "SELECT id, name, family, grade, model_type, created_at FROM materials");

        for (Map<String, Object> material : materials) {
            String id = material.get("id").toString();
            indexDocument(INDEX_MATERIALS, id, material);
        }
        log.info("Reindexed {} materials", materials.size());
    }

    private List<String> getTargetIndices(String type) {
        if (type == null || type.isBlank()) {
            return List.of(INDEX_PLUGINS, INDEX_PROJECTS, INDEX_MATERIALS);
        }
        return switch (type.toLowerCase()) {
            case "plugin", "plugins" -> List.of(INDEX_PLUGINS);
            case "project", "projects" -> List.of(INDEX_PROJECTS);
            case "material", "materials" -> List.of(INDEX_MATERIALS);
            default -> List.of(INDEX_PLUGINS, INDEX_PROJECTS, INDEX_MATERIALS);
        };
    }
}
