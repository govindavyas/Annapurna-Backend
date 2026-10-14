package com.localfood.backend.order;

import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.ApiException;
import com.localfood.backend.config.FeatureProperties;
import com.localfood.backend.config.OrderProperties;
import com.localfood.backend.customer.CustomerPushNotificationService;
import com.localfood.backend.customer.CustomerRealtimeNotifier;
import com.localfood.backend.menu.MenuItem;
import com.localfood.backend.menu.MenuItemRepository;
import com.localfood.backend.admin.AdminRole;
import com.localfood.backend.admin.AdminUserDetails;
import com.localfood.backend.delivery.CompleteDeliveryRequest;
import com.localfood.backend.delivery.DeliveryOrderResponse;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PLACED, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED, OrderStatus.REJECTED),
        OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED, OrderStatus.REJECTED),
        OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP),
        OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED)
    );

    private final MenuItemRepository menuItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventRepository orderEventRepository;
    private final FeatureProperties featureProperties;
    private final OrderProperties orderProperties;
    private final OrderMapper orderMapper;
    private final BranchService branchService;
    private final CustomerRealtimeNotifier customerRealtimeNotifier;
    private final CustomerPushNotificationService customerPushNotificationService;

    public OrderService(
        MenuItemRepository menuItemRepository,
        CustomerOrderRepository customerOrderRepository,
        OrderItemRepository orderItemRepository,
        OrderEventRepository orderEventRepository,
        FeatureProperties featureProperties,
        OrderProperties orderProperties,
        OrderMapper orderMapper,
        BranchService branchService,
        CustomerRealtimeNotifier customerRealtimeNotifier,
        CustomerPushNotificationService customerPushNotificationService
    ) {
        this.menuItemRepository = menuItemRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderEventRepository = orderEventRepository;
        this.featureProperties = featureProperties;
        this.orderProperties = orderProperties;
        this.orderMapper = orderMapper;
        this.branchService = branchService;
        this.customerRealtimeNotifier = customerRealtimeNotifier;
        this.customerPushNotificationService = customerPushNotificationService;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, UUID customerId) {
        com.localfood.backend.common.Coordinates.validate(request.deliveryLatitude(), request.deliveryLongitude(), false);
        Branch branch = branchService.resolveBranchForOrder(request.branchId(), request.deliveryLatitude(), request.deliveryLongitude());
        validateFeatureFlags(request.paymentMode());

        List<UUID> menuIds = request.items().stream().map(OrderLineRequest::menuItemId).toList();
        Map<UUID, MenuItem> itemsById = menuItemRepository.findAllById(menuIds).stream()
            .filter(MenuItem::isAvailable)
            .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        if (itemsById.size() != menuIds.stream().distinct().count()) {
            throw new ApiException("One or more menu items are unavailable");
        }
        if (itemsById.values().stream().anyMatch(item -> !item.isServedBy(branch.getId()))) {
            throw new ApiException("One or more dishes are not served by the selected branch. Review your bag or choose a serving branch");
        }

        CustomerOrder order = new CustomerOrder();
        order.setId(UUID.randomUUID());
        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.customerPhone().trim());
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setDeliveryNotes(request.deliveryNotes() == null ? null : request.deliveryNotes().trim());
        order.setCustomerId(customerId);
        order.setDeliveryLatitude(request.deliveryLatitude());
        order.setDeliveryLongitude(request.deliveryLongitude());
        order.setBranch(branch);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentMode(request.paymentMode());
        order.setPaymentStatus(request.paymentMode() == PaymentMode.COD ? PaymentStatus.NOT_REQUIRED : PaymentStatus.INITIATED);

        int subtotal = 0;
        List<OrderItem> lines = new java.util.ArrayList<>();

        for (OrderLineRequest lineRequest : request.items()) {
            MenuItem menuItem = itemsById.get(lineRequest.menuItemId());
            int lineTotal = menuItem.getPricePaise() * lineRequest.quantity();
            subtotal += lineTotal;

            OrderItem line = new OrderItem();
            line.setMenuItem(menuItem);
            line.setQuantity(lineRequest.quantity());
            line.setUnitPricePaise(menuItem.getPricePaise());
            line.setLineTotalPaise(lineTotal);
            lines.add(line);
        }

        order.setSubtotalPaise(subtotal);
        if (subtotal < branch.getMinimumOrderPaise()) {
            throw new ApiException("Minimum food order for this branch is INR %.2f, excluding delivery".formatted(branch.getMinimumOrderPaise() / 100.0));
        }
        order.setDeliveryFeePaise(branch.getDeliveryFeePaise());
        order.setTotalPaise(subtotal + branch.getDeliveryFeePaise());
        order = customerOrderRepository.save(order);

        for (OrderItem line : lines) {
            line.setOrder(order);
            orderItemRepository.save(line);
        }

        recordEvent(order, OrderStatus.PLACED, "Order placed successfully");

        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus status, java.util.UUID requestedBranchId, java.util.UUID scopedBranchId, boolean isSuperAdmin) {
        if (scopedBranchId != null && requestedBranchId != null && !scopedBranchId.equals(requestedBranchId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own branch orders");
        }
        UUID effectiveBranchId = scopedBranchId != null ? scopedBranchId : requestedBranchId;
        List<CustomerOrder> orders;

        if (effectiveBranchId != null) {
            orders = status == null
                ? customerOrderRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId)
                : customerOrderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(effectiveBranchId, status);
        } else if (isSuperAdmin) {
            orders = status == null
                ? customerOrderRepository.findAllByOrderByCreatedAtDesc()
                : customerOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            throw new ApiException("Branch admin scope is missing");
        }

        return orders.stream()
            .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())))
            .toList();
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus nextStatus, UUID scopedBranchId, boolean isSuperAdmin) {
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));

        assertOrderScope(order, scopedBranchId, isSuperAdmin);

        if (nextStatus == OrderStatus.DELIVERED && order.getPaymentMode() == PaymentMode.COD
            && order.getPaymentStatus() != PaymentStatus.COLLECTED) {
            throw new ApiException("Confirm cash or UPI collection before completing delivery");
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(nextStatus)) {
            throw new ApiException("Invalid status transition from %s to %s".formatted(order.getStatus(), nextStatus));
        }

        order.setStatus(nextStatus);
        if (nextStatus != OrderStatus.REJECTED) {
            order.setRejectReason(null);
        }
        CustomerOrder updated = customerOrderRepository.save(order);
        recordEvent(updated, nextStatus, "Order moved to %s".formatted(nextStatus));
        return orderMapper.toResponse(updated, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason, UUID scopedBranchId, boolean isSuperAdmin) {
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));

        assertOrderScope(order, scopedBranchId, isSuperAdmin);

        if (order.getStatus() == OrderStatus.PREPARING || order.getStatus() == OrderStatus.READY_FOR_PICKUP || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ApiException("Order cannot be cancelled after PREPARING stage as per policy");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setRejectReason(null);
        if (order.getPaymentStatus() == PaymentStatus.CAPTURED) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }

        CustomerOrder updated = customerOrderRepository.save(order);
        recordEvent(updated, OrderStatus.CANCELLED, "Order cancelled: %s".formatted(reason.trim()));
        return orderMapper.toResponse(updated, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse rejectOrder(UUID orderId, String reason, UUID scopedBranchId, boolean isSuperAdmin) {
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));

        assertOrderScope(order, scopedBranchId, isSuperAdmin);

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new ApiException("Only PLACED or ACCEPTED orders can be rejected");
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setRejectReason(reason.trim());
        CustomerOrder updated = customerOrderRepository.save(order);
        recordEvent(updated, OrderStatus.REJECTED, "Order rejected: %s".formatted(reason.trim()));
        return orderMapper.toResponse(updated, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<CustomerDashboardOrderResponse> listOrdersForCustomer(UUID customerId) {
        return customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
            .map(order -> new CustomerDashboardOrderResponse(
                orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())),
                orderEventRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                    .map(event -> new OrderEventResponse(event.getStatus(), event.getMessage(), event.getCreatedAt()))
                    .toList()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerDashboardOrderResponse getOrderForCustomer(UUID customerId, UUID orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));

        if (order.getCustomerId() == null || !order.getCustomerId().equals(customerId)) {
            throw new ApiException("Order not found");
        }

        return new CustomerDashboardOrderResponse(
            orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId)),
            orderEventRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(event -> new OrderEventResponse(event.getStatus(), event.getMessage(), event.getCreatedAt()))
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrderResponse> listDeliveries(UUID branchId, AdminUserDetails actor) {
        return customerOrderRepository.findByBranchIdAndStatusInOrderByCreatedAtDesc(branchId, List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY))
            .stream().filter(order -> order.getPaymentMode() == PaymentMode.COD)
            .filter(order -> actor.getRole() != AdminRole.DELIVERY_STAFF || order.getDispatchedBy() == null || actor.getId().equals(order.getDispatchedBy()))
            .map(order -> actor.getRole() == AdminRole.DELIVERY_STAFF && order.getDispatchedBy() == null
                ? DeliveryOrderResponse.queued(order) : DeliveryOrderResponse.from(order)).toList();
    }

    @Transactional
    public DeliveryOrderResponse startDelivery(UUID orderId, com.localfood.backend.delivery.StartDeliveryRequest request, AdminUserDetails actor) {
        com.localfood.backend.common.Coordinates.validate(request.latitude(), request.longitude(), true);
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));
        assertOrderScope(order, actor.getBranchId(), actor.getRole() == AdminRole.SUPER_ADMIN);
        if ((order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) || order.getPaymentMode() != PaymentMode.COD)
            throw new ApiException("The kitchen must mark this COD order ready for pickup first");
        if (order.getDispatchedBy() != null) {
            if (!actor.getId().equals(order.getDispatchedBy())) throw new ApiException("Another delivery staff member has already started this delivery");
            return DeliveryOrderResponse.from(order);
        }
        order.setDispatchedBy(actor.getId());
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDispatchedAt(OffsetDateTime.now());
        order.setDispatchLatitude(request.latitude()); order.setDispatchLongitude(request.longitude());
        order.setDispatchLocationSource(request.source().name());
        com.localfood.backend.delivery.DeliveryEstimate.calculate(order);
        customerOrderRepository.save(order);
        recordEvent(order, OrderStatus.OUT_FOR_DELIVERY, "Your delivery partner accepted the order and is on the way.");
        return DeliveryOrderResponse.from(order);
    }

    @Transactional
    public DeliveryOrderResponse updateRiderLocation(UUID orderId, com.localfood.backend.delivery.RiderLocationRequest request, AdminUserDetails actor) {
        com.localfood.backend.common.Coordinates.validate(request.latitude(), request.longitude(), true);
        if (!Double.isFinite(request.accuracyMeters())) throw new ApiException("Invalid location accuracy");
        OffsetDateTime now = OffsetDateTime.now();
        if (request.observedAt().isBefore(now.minusSeconds(45)) || request.observedAt().isAfter(now.plusSeconds(15)))
            throw new ApiException("Location is stale. Get a fresh GPS reading");
        CustomerOrder order = ownActiveDelivery(orderId, actor);
        if (order.getRiderLocationAt() != null && !request.observedAt().isAfter(order.getRiderLocationAt())) return DeliveryOrderResponse.from(order);
        if (order.getRiderLocationReceivedAt() != null && order.getRiderLocationReceivedAt().isAfter(now.minusSeconds(3))) return DeliveryOrderResponse.from(order);
        order.setRiderLatitude(request.latitude()); order.setRiderLongitude(request.longitude());
        order.setRiderLocationAt(request.observedAt()); order.setRiderLocationReceivedAt(now);
        order.setRiderAccuracyMeters(request.accuracyMeters()); order.setRiderSharing(true);
        com.localfood.backend.delivery.DeliveryEstimate.calculate(order);
        publishLocationAfterCommit(order);
        return DeliveryOrderResponse.from(customerOrderRepository.save(order));
    }

    @Transactional
    public void pauseRiderLocation(UUID orderId, AdminUserDetails actor) {
        CustomerOrder order = ownActiveDelivery(orderId, actor);
        order.setRiderSharing(false);
        publishLocationAfterCommit(order);
    }

    private CustomerOrder ownActiveDelivery(UUID orderId, AdminUserDetails actor) {
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId).orElseThrow(() -> new ApiException("Order not found"));
        assertOrderScope(order, actor.getBranchId(), actor.getRole() == AdminRole.SUPER_ADMIN);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY || !actor.getId().equals(order.getDispatchedBy()))
            throw new ApiException("Only the assigned delivery partner can share this order's location");
        return order;
    }

    private void publishLocationAfterCommit(CustomerOrder order) {
        UUID customerId = order.getCustomerId(), orderId = order.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { customerRealtimeNotifier.publishDeliveryLocation(customerId, orderId); }
            });
    }

    @Transactional
    public DeliveryOrderResponse completeDelivery(UUID orderId, CompleteDeliveryRequest request, AdminUserDetails actor) {
        CustomerOrder order = customerOrderRepository.findForUpdate(orderId)
            .orElseThrow(() -> new ApiException("Order not found"));
        assertOrderScope(order, actor.getBranchId(), actor.getRole() == AdminRole.SUPER_ADMIN);
        if (actor.getRole() == AdminRole.DELIVERY_STAFF && order.getDispatchedBy() != null && !actor.getId().equals(order.getDispatchedBy()))
            throw new ApiException("This delivery was started by another staff member");
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new ApiException("Confirm that the full payment was received");
        }
        // A retry after a lost response must not create a second collection or event.
        if (order.getStatus() == OrderStatus.DELIVERED && actor.getId().equals(order.getCollectedBy())
            && request.method() == order.getCollectionMethod()) {
            return DeliveryOrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY || order.getPaymentMode() != PaymentMode.COD
            || order.getCollectedAt() != null) {
            throw new ApiException("Only an unpaid COD order out for delivery can be completed");
        }
        if (request.method() == CollectionMethod.UPI) {
            String configuredQr = order.getBranch().getDeliveryQrImageUrl();
            if (configuredQr == null || !configuredQr.equals(request.qrImageUrl())) {
                throw new ApiException("The branch QR is unavailable or changed. Refresh and confirm the correct QR with your admin");
            }
        }
        order.setCollectionMethod(request.method());
        order.setPaymentStatus(PaymentStatus.COLLECTED);
        order.setCollectedAt(OffsetDateTime.now());
        order.setCollectedBy(actor.getId());
        order.setStatus(OrderStatus.DELIVERED);
        order.setRiderSharing(false);
        customerOrderRepository.save(order);
        recordEvent(order, OrderStatus.DELIVERED, request.method() == CollectionMethod.CASH
            ? "Delivered. Cash payment received." : "Delivered. UPI payment manually confirmed by delivery staff.");
        return DeliveryOrderResponse.from(order);
    }

    private void validateFeatureFlags(PaymentMode paymentMode) {
        if (paymentMode == PaymentMode.COD && !featureProperties.codEnabled()) {
            throw new ApiException("COD is currently unavailable");
        }
        if (paymentMode == PaymentMode.ONLINE && !featureProperties.paymentsEnabled()) {
            throw new ApiException("Online payments are currently disabled");
        }
    }

    private void recordEvent(CustomerOrder order, OrderStatus status, String message) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order);
        event.setStatus(status);
        event.setMessage(message);
        event.setCreatedAt(OffsetDateTime.now());
        orderEventRepository.save(event);
        UUID customerId = order.getCustomerId(), orderId = order.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {
                    customerRealtimeNotifier.publishOrderUpdate(customerId, orderId, status, message);
                    customerPushNotificationService.sendOrderStatusUpdate(customerId, orderId, status, message);
                }
            });
    }

    private void assertOrderScope(CustomerOrder order, UUID scopedBranchId, boolean isSuperAdmin) {
        if (isSuperAdmin) {
            return;
        }
        if (scopedBranchId == null) {
            throw new ApiException("Branch admin scope is missing");
        }
        if (!scopedBranchId.equals(order.getBranch().getId())) {
            throw new ApiException("Order does not belong to your branch");
        }
    }
}
