package com.localfood.backend.customer;

import com.localfood.backend.order.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerInsightResponse(
    UUID customerId,
    String name,
    String phone,
    OffsetDateTime registeredAt,
    int totalOrders,
    int activeOrders,
    int completedOrders,
    int issueOrders,
    int totalSpentPaise,
    int averageOrderValuePaise,
    OffsetDateTime lastOrderAt,
    OrderStatus lastOrderStatus,
    UUID preferredBranchId,
    String preferredBranchName,
    String priorityTier,
    String priorityReason
) {
}
