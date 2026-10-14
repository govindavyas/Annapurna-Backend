package com.localfood.backend.order;

import java.util.List;

public record CustomerDashboardOrderResponse(
    OrderResponse order,
    List<OrderEventResponse> notifications
) {
}
