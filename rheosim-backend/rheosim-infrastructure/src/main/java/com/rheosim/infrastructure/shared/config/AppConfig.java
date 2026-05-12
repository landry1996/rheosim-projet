package com.rheosim.infrastructure.shared.config;

import com.rheosim.infrastructure.identity.config.CorsProperties;
import com.rheosim.infrastructure.identity.config.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class AppConfig {
}
