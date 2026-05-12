package com.rheosim.infrastructure.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_USER_EVENTS = "rheosim.identity.user-events";
    public static final String TOPIC_SIMULATION_JOBS = "rheosim.simulation.jobs";
    public static final String TOPIC_SIMULATION_STATUS = "rheosim.simulation.status";
    public static final String TOPIC_EXPERIMENT_EVENTS = "rheosim.experiment.events";

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(TOPIC_USER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic simulationJobsTopic() {
        return TopicBuilder.name(TOPIC_SIMULATION_JOBS)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic simulationStatusTopic() {
        return TopicBuilder.name(TOPIC_SIMULATION_STATUS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic experimentEventsTopic() {
        return TopicBuilder.name(TOPIC_EXPERIMENT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
