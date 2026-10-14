package com.localfood.backend.customer;

public record CustomerPushSubscriptionResponse(
    PushPlatform platform,
    String token,
    boolean active,
    String message
) {
}
