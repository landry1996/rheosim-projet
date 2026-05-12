package com.rheosim.infrastructure.experiment.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rheosim.domain.experiment.event.DatasetUploadedEvent;
import com.rheosim.domain.experiment.event.DatasetValidatedEvent;
import com.rheosim.infrastructure.shared.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatasetEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DatasetEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishUploaded(DatasetUploadedEvent event) {
        String payload = serialize(Map.of(
                "type", "DATASET_UPLOADED",
                "datasetId", event.getDatasetId().toString(),
                "projectId", event.getProjectId().toString(),
                "uploadedBy", event.getUploadedBy().toString(),
                "fileName", event.getFileName(),
                "occurredAt", event.getOccurredAt().toString()
        ));
        kafkaTemplate.send(KafkaConfig.TOPIC_EXPERIMENT_EVENTS, event.getDatasetId().toString(), payload);
    }

    public void publishValidated(DatasetValidatedEvent event) {
        String payload = serialize(Map.of(
                "type", "DATASET_VALIDATED",
                "datasetId", event.getDatasetId().toString(),
                "projectId", event.getProjectId().toString(),
                "valid", String.valueOf(event.isValid()),
                "errorCount", String.valueOf(event.getErrorCount()),
                "occurredAt", event.getOccurredAt().toString()
        ));
        kafkaTemplate.send(KafkaConfig.TOPIC_EXPERIMENT_EVENTS, event.getDatasetId().toString(), payload);
    }

    private String serialize(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
