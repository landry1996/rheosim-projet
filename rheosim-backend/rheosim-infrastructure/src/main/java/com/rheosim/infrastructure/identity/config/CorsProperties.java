package com.rheosim.infrastructure.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rheosim.security.cors")
public record CorsProperties(
        String allowedOrigins,
        String allowedMethods,
        String allowedHeaders,
        boolean allowCredentials
) {}
