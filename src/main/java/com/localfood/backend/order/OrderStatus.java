package com.localfood.backend.order;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED
}
