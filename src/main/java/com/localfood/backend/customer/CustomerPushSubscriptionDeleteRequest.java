package com.localfood.backend.customer;

import jakarta.validation.constraints.NotBlank;

public record CustomerPushSubscriptionDeleteRequest(@NotBlank String token) {
}
