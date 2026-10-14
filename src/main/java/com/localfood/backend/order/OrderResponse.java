package com.localfood.backend.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID orderId,
    UUID branchId,
    String branchCode,
    String branchName,
    String customerName,
    String customerPhone,
    String deliveryAddress,
    Double deliveryLatitude,
    Double deliveryLongitude,
    Integer subtotalPaise,
    Integer deliveryFeePaise,
    Integer totalPaise,
    String currency,
    OrderStatus status,
    PaymentMode paymentMode,
    PaymentStatus paymentStatus,
    CollectionMethod collectionMethod,
    OffsetDateTime collectedAt,
    String cancelReason,
    String rejectReason,
    OffsetDateTime createdAt,
    List<OrderLineResponse> items,
    String deliveryNotes,
    com.localfood.backend.delivery.DeliveryEstimateResponse deliveryEstimate
) {
}
