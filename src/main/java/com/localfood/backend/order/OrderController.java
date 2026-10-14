package com.localfood.backend.order;

import com.localfood.backend.customer.CustomerAccount;
import com.localfood.backend.customer.CustomerAuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerAuthService customerAuthService;

    public OrderController(OrderService orderService, CustomerAuthService customerAuthService) {
        this.orderService = orderService;
        this.customerAuthService = customerAuthService;
    }

    @PostMapping
    public OrderResponse placeOrder(
        @Valid @RequestBody PlaceOrderRequest request,
        @RequestHeader(value = "X-Customer-Token", required = false) String customerToken
    ) {
        CustomerAccount customer = customerAuthService.requireCustomer(customerToken);
        UUID customerId = customer.getId();
        return orderService.placeOrder(request, customerId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId,
        @RequestHeader(value = "X-Customer-Token", required = false) String customerToken) {
        CustomerAccount customer = customerAuthService.requireCustomer(customerToken);
        return orderService.getOrderForCustomer(customer.getId(), orderId).order();
    }
}
