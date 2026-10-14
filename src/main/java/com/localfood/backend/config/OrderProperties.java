package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(
    String currency,
    Integer deliveryFeePaise
) {
}
