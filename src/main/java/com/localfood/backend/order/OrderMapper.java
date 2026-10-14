package com.localfood.backend.order;

import com.localfood.backend.config.OrderProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final OrderProperties orderProperties;

    public OrderMapper(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }

    public OrderResponse toResponse(CustomerOrder order, List<OrderItem> items) {
        List<OrderLineResponse> lines = items.stream()
            .map(item -> new OrderLineResponse(
                item.getMenuItem().getId(),
                item.getMenuItem().getName(),
                item.getQuantity(),
                item.getUnitPricePaise(),
                item.getLineTotalPaise()
            ))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getBranch().getId(),
            order.getBranch().getCode(),
            order.getBranch().getName(),
            order.getCustomerName(),
            order.getCustomerPhone(),
            order.getDeliveryAddress(),
            order.getDeliveryLatitude(),
            order.getDeliveryLongitude(),
            order.getSubtotalPaise(),
            order.getDeliveryFeePaise(),
            order.getTotalPaise(),
            orderProperties.currency(),
            order.getStatus(),
            order.getPaymentMode(),
            order.getPaymentStatus(),
            order.getCollectionMethod(),
            order.getCollectedAt(),
            order.getCancelReason(),
            order.getRejectReason(),
            order.getCreatedAt(),
            lines,
            order.getDeliveryNotes(),
            com.localfood.backend.delivery.DeliveryEstimateResponse.from(order)
        );
    }
}
