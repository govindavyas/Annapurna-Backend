package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.customer")
public record CustomerAuthProperties(
    long sessionDays
) {
}
