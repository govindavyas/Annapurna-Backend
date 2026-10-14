package com.localfood.backend.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderLineRequest(
    @NotNull UUID menuItemId,
    @NotNull @Min(1) Integer quantity
) {
}
