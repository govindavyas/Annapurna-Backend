package com.localfood.backend.delivery;

import com.localfood.backend.admin.AdminAuthContext;
import com.localfood.backend.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/delivery")
public class DeliveryController {
    private final OrderService orders;
    private final AdminAuthContext auth;
    private final DeliveryPaymentService payments;

    public DeliveryController(OrderService orders, AdminAuthContext auth, DeliveryPaymentService payments) {
        this.orders = orders;
        this.auth = auth;
        this.payments = payments;
    }

    @GetMapping("/orders")
    public List<DeliveryOrderResponse> list(@RequestParam(required = false) UUID branchId) {
        UUID scope = payments.get(branchId).branchId();
        return orders.listDeliveries(scope, auth.requireAdmin());
    }

    @GetMapping("/payment-settings")
    public DeliveryPaymentSettings settings(@RequestParam(required = false) UUID branchId) {
        return payments.get(branchId);
    }

    @PostMapping("/orders/{orderId}/complete")
    public DeliveryOrderResponse complete(@PathVariable UUID orderId, @Valid @RequestBody CompleteDeliveryRequest request) {
        return orders.completeDelivery(orderId, request, auth.requireAdmin());
    }

    @PostMapping("/orders/{orderId}/start")
    public DeliveryOrderResponse start(@PathVariable UUID orderId, @Valid @RequestBody StartDeliveryRequest request) {
        return orders.startDelivery(orderId, request, auth.requireAdmin());
    }
    @PutMapping("/orders/{orderId}/location")
    public DeliveryOrderResponse location(@PathVariable UUID orderId, @Valid @RequestBody RiderLocationRequest request) {
        return orders.updateRiderLocation(orderId, request, auth.requireAdmin());
    }
    @PostMapping("/orders/{orderId}/location/pause")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void pause(@PathVariable UUID orderId) { orders.pauseRiderLocation(orderId, auth.requireAdmin()); }
}
