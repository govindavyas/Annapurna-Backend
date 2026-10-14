package com.localfood.backend.delivery;

import com.localfood.backend.order.CollectionMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteDeliveryRequest(
    @NotNull CollectionMethod method,
    @AssertTrue(message = "Confirm that the full payment was received") @NotNull Boolean confirmed,
    @Size(max = 500) String qrImageUrl
) {
}
