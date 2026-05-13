package com.rheosim.infrastructure.search.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchEventListener {

    private static final Logger log = LoggerFactory.getLogger(SearchEventListener.class);

    private final ElasticsearchIndexer indexer;
    private final ObjectMapper objectMapper;

    public SearchEventListener(ElasticsearchIndexer indexer, ObjectMapper objectMapper) {
        this.indexer = indexer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "rheosim.projects", groupId = "search-indexer")
    public void onProjectEvent(String message) {
        processEvent(message, "rheosim-projects");
    }

    @KafkaListener(topics = "rheosim.materials", groupId = "search-indexer")
    public void onMaterialEvent(String message) {
        processEvent(message, "rheosim-materials");
    }

    @KafkaListener(topics = "rheosim.plugins", groupId = "search-indexer")
    public void onPluginEvent(String message) {
        processEvent(message, "rheosim-plugins");
    }

    private void processEvent(String message, String indexName) {
        try {
            Map<String, Object> event = objectMapper.readValue(
                    message, new TypeReference<Map<String, Object>>() {});

            String eventType = (String) event.get("event_type");
            String entityId = (String) event.get("id");
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");

            switch (eventType) {
                case "CREATED", "UPDATED" -> {
                    if (payload != null && entityId != null) {
                        indexer.indexDocument(indexName, entityId, payload);
                        log.debug("Indexed {} in {} ({})", entityId, indexName, eventType);
                    }
                }
                case "DELETED" -> {
                    if (entityId != null) {
                        indexer.deleteDocument(indexName, entityId);
                        log.debug("Deleted {} from {} ({})", entityId, indexName, eventType);
                    }
                }
                default -> log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process search event for index {}: {}", indexName, e.getMessage());
        }
    }
}
