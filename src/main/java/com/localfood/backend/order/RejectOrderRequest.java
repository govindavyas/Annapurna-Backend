package com.localfood.backend.order;

import jakarta.validation.constraints.NotBlank;

public record RejectOrderRequest(@NotBlank String reason) {
}
