package com.localfood.backend.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerPushSubscriptionRequest(
    @NotNull PushPlatform platform,
    @NotBlank String token
) {
}
