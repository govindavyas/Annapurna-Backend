package com.localfood.backend.order;

import java.util.UUID;

public record OrderLineResponse(
    UUID menuItemId,
    String name,
    Integer quantity,
    Integer unitPricePaise,
    Integer lineTotalPaise
) {
}
