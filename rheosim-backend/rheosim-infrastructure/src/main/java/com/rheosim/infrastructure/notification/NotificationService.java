package com.rheosim.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifySimulationCompleted(UUID userId, UUID simulationId, String status) {
        var payload = Map.of(
                "type", "SIMULATION_COMPLETED",
                "simulationId", simulationId.toString(),
                "status", status,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
        log.debug("Sent simulation notification to user {}", userId);
    }

    public void notifyInvitation(UUID userId, UUID organizationId, String organizationName) {
        var payload = Map.of(
                "type", "INVITATION_RECEIVED",
                "organizationId", organizationId.toString(),
                "organizationName", organizationName,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
        log.debug("Sent invitation notification to user {}", userId);
    }

    public void notifyProjectShared(UUID userId, UUID projectId, String projectName, String role) {
        var payload = Map.of(
                "type", "PROJECT_SHARED",
                "projectId", projectId.toString(),
                "projectName", projectName,
                "role", role,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload);
        log.debug("Sent project shared notification to user {}", userId);
    }

    public void broadcastSimulationProgress(UUID simulationId, int progress, String phase) {
        var payload = Map.of(
                "type", "SIMULATION_PROGRESS",
                "simulationId", simulationId.toString(),
                "progress", String.valueOf(progress),
                "phase", phase,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/simulations/" + simulationId, payload);
    }
}
