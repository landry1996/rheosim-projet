package com.rheosim.infrastructure.collaboration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketCollaborationConfig implements WebSocketConfigurer {

    private final CollaborationWebSocketHandler collaborationHandler;

    public WebSocketCollaborationConfig(CollaborationWebSocketHandler collaborationHandler) {
        this.collaborationHandler = collaborationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collaborationHandler, "/ws/collaboration/{documentId}")
                .setAllowedOrigins("*");
    }
}
