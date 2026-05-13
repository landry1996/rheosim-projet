package com.rheosim.infrastructure.collaboration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class CollaborationWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CollaborationWebSocketHandler.class);
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64KB max per CRDT update
    private static final int MAX_SESSIONS_PER_DOCUMENT = 50;
    private static final int MAX_TEXT_MESSAGE_SIZE = 4 * 1024; // 4KB for awareness

    private final Map<String, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();
    private final Map<String, byte[]> documentStates = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String documentId = extractDocumentId(session);
        Set<WebSocketSession> sessions = documentSessions.computeIfAbsent(documentId, k -> new CopyOnWriteArraySet<>());
        if (sessions.size() >= MAX_SESSIONS_PER_DOCUMENT) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.error("Failed to reject session", e);
            }
            return;
        }
        sessions.add(session);
        session.setTextMessageSizeLimit(MAX_TEXT_MESSAGE_SIZE);
        session.setBinaryMessageSizeLimit(MAX_MESSAGE_SIZE);
        log.info("Client connected to document {}: {}", documentId, session.getId());

        // Send current state to new client
        byte[] state = documentStates.get(documentId);
        if (state != null) {
            try {
                session.sendMessage(new BinaryMessage(state));
            } catch (IOException e) {
                log.error("Failed to send initial state", e);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String documentId = extractDocumentId(session);
        byte[] payload = message.getPayload().array();

        if (payload.length > MAX_MESSAGE_SIZE) {
            log.warn("Oversized binary message from session {}, rejecting", session.getId());
            return;
        }

        documentStates.put(documentId, payload);

        // Broadcast to all other clients in the same document
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            for (WebSocketSession peer : sessions) {
                if (peer.isOpen() && !peer.getId().equals(session.getId())) {
                    try {
                        peer.sendMessage(new BinaryMessage(payload));
                    } catch (IOException e) {
                        log.error("Failed to broadcast to peer {}", peer.getId(), e);
                    }
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Awareness messages (JSON)
        String documentId = extractDocumentId(session);
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            for (WebSocketSession peer : sessions) {
                if (peer.isOpen() && !peer.getId().equals(session.getId())) {
                    try {
                        peer.sendMessage(message);
                    } catch (IOException e) {
                        log.error("Failed to forward awareness", e);
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String documentId = extractDocumentId(session);
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                documentSessions.remove(documentId);
            }
        }
        log.info("Client disconnected from document {}: {}", documentId, session.getId());
    }

    private String extractDocumentId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "default";
    }

    public int getActiveConnections(String documentId) {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        return sessions != null ? sessions.size() : 0;
    }
}
