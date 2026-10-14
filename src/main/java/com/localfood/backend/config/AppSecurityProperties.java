package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.admin")
public record AppSecurityProperties(
    String username,
    String password,
    String totpSecret
) {
}
