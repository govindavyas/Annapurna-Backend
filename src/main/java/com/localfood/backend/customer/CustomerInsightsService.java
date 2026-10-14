package com.localfood.backend.customer;

import com.localfood.backend.common.ApiException;
import com.localfood.backend.order.CustomerOrder;
import com.localfood.backend.order.CustomerOrderRepository;
import com.localfood.backend.order.OrderStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerInsightsService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerAccountRepository customerAccountRepository;

    public CustomerInsightsService(
        CustomerOrderRepository customerOrderRepository,
        CustomerAccountRepository customerAccountRepository
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerAccountRepository = customerAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerInsightResponse> listInsights(UUID requestedBranchId, UUID scopedBranchId, boolean isSuperAdmin, String query) {
        if (scopedBranchId != null && requestedBranchId != null && !scopedBranchId.equals(requestedBranchId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own branch customers");
        }
        UUID effectiveBranchId = scopedBranchId != null ? scopedBranchId : requestedBranchId;
        List<CustomerOrder> scopedOrders;

        if (effectiveBranchId != null) {
            scopedOrders = customerOrderRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId);
        } else if (isSuperAdmin) {
            scopedOrders = customerOrderRepository.findAllByOrderByCreatedAtDesc();
        } else {
            throw new ApiException("Branch admin scope is missing");
        }

        Map<UUID, InsightAccumulator> metricsByCustomer = new HashMap<>();

        for (CustomerOrder order : scopedOrders) {
            if (order.getCustomerId() == null) {
                continue;
            }

            InsightAccumulator accumulator = metricsByCustomer.computeIfAbsent(order.getCustomerId(), ignored -> new InsightAccumulator());
            accumulator.ingest(order);
        }

        if (metricsByCustomer.isEmpty()) {
            return List.of();
        }

        Map<UUID, CustomerAccount> accountsById = customerAccountRepository.findAllById(metricsByCustomer.keySet()).stream()
            .collect(java.util.stream.Collectors.toMap(CustomerAccount::getId, account -> account));

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CustomerInsightResponse> response = new ArrayList<>();

        for (Map.Entry<UUID, InsightAccumulator> entry : metricsByCustomer.entrySet()) {
            UUID customerId = entry.getKey();
            CustomerAccount account = accountsById.get(customerId);
            if (account == null) {
                continue;
            }

            InsightAccumulator accumulator = entry.getValue();
            Priority priority = derivePriority(accumulator);

            CustomerInsightResponse row = new CustomerInsightResponse(
                customerId,
                account.getName(),
                account.getPhone(),
                account.getCreatedAt(),
                accumulator.totalOrders,
                accumulator.activeOrders,
                accumulator.completedOrders,
                accumulator.issueOrders,
                accumulator.totalSpentPaise,
                accumulator.totalOrders == 0 ? 0 : accumulator.totalSpentPaise / accumulator.totalOrders,
                accumulator.lastOrderAt,
                accumulator.lastOrderStatus,
                accumulator.preferredBranchId,
                accumulator.preferredBranchName,
                priority.tier,
                priority.reason
            );

            if (!normalizedQuery.isBlank() && !matchesQuery(row, normalizedQuery)) {
                continue;
            }
            response.add(row);
        }

        response.sort(Comparator
            .comparingInt((CustomerInsightResponse item) -> priorityRank(item.priorityTier()))
            .thenComparingInt(CustomerInsightResponse::totalOrders)
            .thenComparingInt(CustomerInsightResponse::totalSpentPaise)
            .thenComparing(CustomerInsightResponse::lastOrderAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());

        return response;
    }

    private boolean matchesQuery(CustomerInsightResponse row, String query) {
        String status = row.lastOrderStatus() == null ? "" : row.lastOrderStatus().name().toLowerCase(Locale.ROOT);
        return row.name().toLowerCase(Locale.ROOT).contains(query)
            || row.phone().toLowerCase(Locale.ROOT).contains(query)
            || row.priorityTier().toLowerCase(Locale.ROOT).contains(query)
            || status.contains(query);
    }

    private Priority derivePriority(InsightAccumulator accumulator) {
        if (accumulator.totalOrders >= 12 || accumulator.totalSpentPaise >= 120_000) {
            return new Priority("VIP", "High lifetime value, prioritize exclusive offers");
        }
        if (accumulator.totalOrders >= 6 || accumulator.totalSpentPaise >= 60_000) {
            return new Priority("LOYAL", "Repeat customer, prioritize retention offers");
        }
        if (accumulator.totalOrders >= 3) {
            return new Priority("GROWING", "Engaged customer, nudge with targeted combo offers");
        }
        return new Priority("NEW", "Early lifecycle, prioritize welcome discounts");
    }

    private int priorityRank(String tier) {
        return switch (tier) {
            case "VIP" -> 4;
            case "LOYAL" -> 3;
            case "GROWING" -> 2;
            default -> 1;
        };
    }

    private static final class InsightAccumulator {
        private int totalOrders;
        private int activeOrders;
        private int completedOrders;
        private int issueOrders;
        private int totalSpentPaise;
        private OffsetDateTime lastOrderAt;
        private OrderStatus lastOrderStatus;
        private UUID preferredBranchId;
        private String preferredBranchName;
        private int preferredBranchCount;
        private final Map<UUID, Integer> branchCounts = new HashMap<>();

        private void ingest(CustomerOrder order) {
            totalOrders += 1;
            totalSpentPaise += order.getTotalPaise() == null ? 0 : order.getTotalPaise();

            OrderStatus status = order.getStatus();
            if (status == OrderStatus.PLACED || status == OrderStatus.ACCEPTED || status == OrderStatus.PREPARING || status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.OUT_FOR_DELIVERY) {
                activeOrders += 1;
            } else if (status == OrderStatus.DELIVERED) {
                completedOrders += 1;
            } else if (status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED) {
                issueOrders += 1;
            }

            if (lastOrderAt == null || (order.getCreatedAt() != null && order.getCreatedAt().isAfter(lastOrderAt))) {
                lastOrderAt = order.getCreatedAt();
                lastOrderStatus = order.getStatus();
            }

            UUID branchId = order.getBranch() == null ? null : order.getBranch().getId();
            if (branchId == null) {
                return;
            }

            int count = branchCounts.compute(branchId, (ignored, old) -> old == null ? 1 : old + 1);
            if (count > preferredBranchCount) {
                preferredBranchCount = count;
                preferredBranchId = branchId;
                preferredBranchName = order.getBranch().getName();
            }
        }
    }

    private record Priority(String tier, String reason) {
    }
}
