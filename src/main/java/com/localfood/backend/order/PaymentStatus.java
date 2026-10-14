package com.localfood.backend.order;

public enum PaymentStatus {
    NOT_REQUIRED,
    COLLECTED,
    INITIATED,
    CAPTURED,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
