package com.localfood.backend.order;

import com.localfood.backend.admin.AdminAuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final AdminAuthContext adminAuthContext;

    public AdminOrderController(OrderService orderService, AdminAuthContext adminAuthContext) {
        this.orderService = orderService;
        this.adminAuthContext = adminAuthContext;
    }

    @GetMapping
    public List<OrderResponse> listOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) UUID branchId
    ) {
        UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        return orderService.listOrders(status, branchId, scopedBranchId, adminAuthContext.isSuperAdmin());
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(@PathVariable UUID orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        return orderService.updateStatus(orderId, request.status(), scopedBranchId, adminAuthContext.isSuperAdmin());
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        return orderService.cancelOrder(orderId, request.reason(), scopedBranchId, adminAuthContext.isSuperAdmin());
    }

    @PostMapping("/{orderId}/reject")
    public OrderResponse rejectOrder(@PathVariable UUID orderId, @Valid @RequestBody RejectOrderRequest request) {
        UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        return orderService.rejectOrder(orderId, request.reason(), scopedBranchId, adminAuthContext.isSuperAdmin());
    }
}
