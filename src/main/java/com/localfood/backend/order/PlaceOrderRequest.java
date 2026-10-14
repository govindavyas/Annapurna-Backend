package com.localfood.backend.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
    UUID branchId,
    @NotBlank String customerName,
    @NotBlank String customerPhone,
    @NotBlank @jakarta.validation.constraints.Size(max=500) String deliveryAddress,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double deliveryLatitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double deliveryLongitude,
    @NotNullPaymentMode PaymentMode paymentMode,
    @Valid @NotEmpty List<OrderLineRequest> items,
    @jakarta.validation.constraints.Size(max=300) String deliveryNotes
) {
}
