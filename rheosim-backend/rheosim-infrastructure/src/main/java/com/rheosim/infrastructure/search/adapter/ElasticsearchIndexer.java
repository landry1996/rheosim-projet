package com.rheosim.infrastructure.search.adapter;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.rheosim.infrastructure.search.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticsearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexer.class);

    private static final String INDEX_PLUGINS = "rheosim-plugins";
    private static final String INDEX_PROJECTS = "rheosim-projects";
    private static final String INDEX_MATERIALS = "rheosim-materials";

    private final ElasticsearchClient esClient;
    private final JdbcTemplate jdbcTemplate;

    public ElasticsearchIndexer(ElasticsearchClient esClient, JdbcTemplate jdbcTemplate) {
        this.esClient = esClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureIndices() {
        createIndexIfNotExists(INDEX_PLUGINS);
        createIndexIfNotExists(INDEX_PROJECTS);
        createIndexIfNotExists(INDEX_MATERIALS);
    }

    public SearchResult search(String query, String type, int page, int size) {
        try {
            List<String> indices = getTargetIndices(type);

            SearchResponse<Map> response = esClient.search(s -> s
                    .index(indices)
                    .from(page * size)
                    .size(size)
                    .query(buildQuery(query))
                    .highlight(h -> h
                            .fields("name", HighlightField.of(hf -> hf))
                            .fields("description", HighlightField.of(hf -> hf))
                            .preTags("<mark>")
                            .postTags("</mark>"))
                    .aggregations("by_type", a -> a
                            .terms(t -> t.field("_index"))),
                    Map.class);

            TotalHits total = response.hits().total();
            long totalHits = total != null ? total.value() : 0;

            List<SearchResult.SearchHit> hits = response.hits().hits().stream()
                    .map(this::mapHit)
                    .collect(Collectors.toList());

            Map<String, Long> facets = new HashMap<>();
            if (response.aggregations().containsKey("by_type")) {
                response.aggregations().get("by_type").sterms().buckets().array()
                        .forEach(b -> facets.put(b.key().stringValue(), b.docCount()));
            }

            return new SearchResult(hits, totalHits, page, size, facets);
        } catch (IOException e) {
            log.error("Search failed", e);
            return new SearchResult(List.of(), 0, page, size, Map.of());
        }
    }

    public List<String> suggest(String query, int limit) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(List.of(INDEX_PLUGINS, INDEX_PROJECTS, INDEX_MATERIALS))
                    .size(limit)
                    .query(q -> q.matchPhrasePrefix(m -> m
                            .field("name")
                            .query(query)))
                    .source(src -> src.filter(f -> f.includes(List.of("name")))),
                    Map.class);

            return response.hits().hits().stream()
                    .map(hit -> (String) hit.source().get("name"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Suggest failed", e);
            return List.of();
        }
    }

    public void indexDocument(String index, String id, Map<String, Object> document) {
        try {
            esClient.index(i -> i.index(index).id(id).document(document));
        } catch (IOException e) {
            log.error("Failed to index document {} in {}", id, index, e);
        }
    }

    public void deleteDocument(String index, String id) {
        try {
            esClient.delete(d -> d.index(index).id(id));
        } catch (IOException e) {
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

    private Query buildQuery(String queryText) {
        return Query.of(q -> q.multiMatch(MultiMatchQuery.of(m -> m
                .query(queryText)
                .fields(List.of("name^3", "description^2", "tags", "family", "grade", "author"))
                .fuzziness("AUTO")
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields))));
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

    private SearchResult.SearchHit mapHit(Hit<Map> hit) {
        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights = hit.highlight();
        }
        return new SearchResult.SearchHit(
                hit.id(),
                hit.index(),
                hit.score(),
                hit.source(),
                highlights
        );
    }

    private void createIndexIfNotExists(String indexName) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                esClient.indices().create(c -> c
                        .index(indexName)
                        .settings(s -> s
                                .numberOfShards("2")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("french_english", an -> an
                                                .custom(cu -> cu
                                                        .tokenizer("standard")
                                                        .filter(List.of("lowercase", "asciifolding")))))));
                log.info("Created index: {}", indexName);
            }
        } catch (IOException e) {
            log.error("Failed to create index: {}", indexName, e);
        }
    }
}
