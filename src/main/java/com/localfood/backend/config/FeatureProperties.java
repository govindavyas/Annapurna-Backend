package com.localfood.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.features")
public record FeatureProperties(
    boolean codEnabled,
    boolean paymentsEnabled,
    boolean smsOtpEnabled,
    boolean whatsappEnabled
) {
}
