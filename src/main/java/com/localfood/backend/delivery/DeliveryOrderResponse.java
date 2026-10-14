package com.localfood.backend.delivery;

import com.localfood.backend.order.CustomerOrder;
import com.localfood.backend.order.CollectionMethod;
import com.localfood.backend.order.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderResponse(
    UUID orderId, String branchName, String customerName, String customerPhone,
    String deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
    int totalPaise, OrderStatus status, CollectionMethod collectionMethod, OffsetDateTime collectedAt,
    String deliveryNotes, UUID dispatchedBy, DeliveryEstimateResponse deliveryEstimate
) {
    public static DeliveryOrderResponse queued(CustomerOrder order) {
        return new DeliveryOrderResponse(order.getId(), order.getBranch().getName(), null, null, null, null, null,
            order.getTotalPaise(), order.getStatus(), null, null, null, null, null);
    }
    public static DeliveryOrderResponse from(CustomerOrder order) {
        return new DeliveryOrderResponse(order.getId(), order.getBranch().getName(),
            order.getCustomerName(), order.getCustomerPhone(), order.getDeliveryAddress(),
            order.getDeliveryLatitude(), order.getDeliveryLongitude(), order.getTotalPaise(),
            order.getStatus(), order.getCollectionMethod(), order.getCollectedAt(), order.getDeliveryNotes(),
            order.getDispatchedBy(), DeliveryEstimateResponse.from(order));
    }
}
