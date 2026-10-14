package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push")
public record PushProperties(
    boolean enabled,
    FcmProperties fcm
) {

    public record FcmProperties(
        String serviceAccountJsonBase64,
        String androidChannelId
    ) {
    }
}
