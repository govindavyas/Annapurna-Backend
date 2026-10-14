package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
    String uploadDir,
    String publicBasePath
) {
}
