package com.rheosim.infrastructure.collaboration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketCollaborationConfig implements WebSocketConfigurer {

    private final CollaborationWebSocketHandler collaborationHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${rheosim.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public WebSocketCollaborationConfig(CollaborationWebSocketHandler collaborationHandler,
                                         WebSocketAuthInterceptor authInterceptor) {
        this.collaborationHandler = collaborationHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collaborationHandler, "/ws/collaboration/{documentId}")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
