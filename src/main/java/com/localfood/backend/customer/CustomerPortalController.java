package com.localfood.backend.customer;

import com.localfood.backend.order.CustomerDashboardOrderResponse;
import com.localfood.backend.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerPortalController {

    private final CustomerAuthService customerAuthService;
    private final OrderService orderService;
    private final CustomerRealtimeNotifier customerRealtimeNotifier;
    private final CustomerPushSubscriptionService customerPushSubscriptionService;

    public CustomerPortalController(
        CustomerAuthService customerAuthService,
        OrderService orderService,
        CustomerRealtimeNotifier customerRealtimeNotifier,
        CustomerPushSubscriptionService customerPushSubscriptionService
    ) {
        this.customerAuthService = customerAuthService;
        this.orderService = orderService;
        this.customerRealtimeNotifier = customerRealtimeNotifier;
        this.customerPushSubscriptionService = customerPushSubscriptionService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
        @RequestHeader(value = "X-Customer-Token", required = false) String headerToken,
        @RequestParam(required = false) String token
    ) {
        String effectiveToken = headerToken != null && !headerToken.isBlank() ? headerToken : token;
        CustomerAccount account = customerAuthService.requireCustomer(effectiveToken);
        return customerRealtimeNotifier.subscribe(account.getId());
    }

    @PostMapping("/push-subscriptions")
    public CustomerPushSubscriptionResponse registerPushSubscription(
        @RequestHeader("X-Customer-Token") String token,
        @Valid @RequestBody CustomerPushSubscriptionRequest request
    ) {
        CustomerAccount account = customerAuthService.requireCustomer(token);
        return customerPushSubscriptionService.register(account.getId(), request);
    }

    @DeleteMapping("/push-subscriptions")
    public CustomerPushSubscriptionResponse unregisterPushSubscription(
        @RequestHeader("X-Customer-Token") String token,
        @Valid @RequestBody CustomerPushSubscriptionDeleteRequest request
    ) {
        CustomerAccount account = customerAuthService.requireCustomer(token);
        return customerPushSubscriptionService.unregister(account.getId(), request.token());
    }

    @GetMapping("/profile")
    public CustomerProfileResponse profile(@RequestHeader("X-Customer-Token") String token) {
        CustomerAccount account = customerAuthService.requireCustomer(token);
        return CustomerProfileResponse.from(account);
    }

    @GetMapping("/orders")
    public List<CustomerDashboardOrderResponse> orders(@RequestHeader("X-Customer-Token") String token) {
        CustomerAccount account = customerAuthService.requireCustomer(token);
        return orderService.listOrdersForCustomer(account.getId());
    }

    @GetMapping("/orders/{orderId}")
    public CustomerDashboardOrderResponse order(
        @RequestHeader("X-Customer-Token") String token,
        @PathVariable UUID orderId
    ) {
        CustomerAccount account = customerAuthService.requireCustomer(token);
        return orderService.getOrderForCustomer(account.getId(), orderId);
    }
}
