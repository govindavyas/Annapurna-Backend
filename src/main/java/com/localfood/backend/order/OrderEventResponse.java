package com.localfood.backend.order;

import java.time.OffsetDateTime;

public record OrderEventResponse(
    OrderStatus status,
    String message,
    OffsetDateTime createdAt
) {
}
