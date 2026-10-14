package com.localfood.backend.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfood.backend.order.OrderStatus;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CustomerRealtimeNotifier {

    private final Map<UUID, List<SseEmitter>> customerEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public CustomerRealtimeNotifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(UUID customerId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> emitters = customerEmitters.computeIfAbsent(customerId, ignored -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(customerId, emitter));
        emitter.onTimeout(() -> removeEmitter(customerId, emitter));
        emitter.onError(ex -> removeEmitter(customerId, emitter));

        sendPayload(customerId, emitter, new CustomerRealtimeEvent("connected", null, null, "Connected to customer realtime stream", OffsetDateTime.now().toString()));
        return emitter;
    }

    public void publishOrderUpdate(UUID customerId, UUID orderId, OrderStatus status, String message) {
        if (customerId == null) {
            return;
        }
        CustomerRealtimeEvent event = new CustomerRealtimeEvent(
            "order-update",
            orderId,
            status == null ? null : status.name(),
            message,
            OffsetDateTime.now().toString()
        );
        publishToCustomer(customerId, event);
    }

    public void publishDeliveryLocation(UUID customerId, UUID orderId) {
        if (customerId != null) publishToCustomer(customerId, new CustomerRealtimeEvent(
            "delivery-location", orderId, "OUT_FOR_DELIVERY", "Delivery location updated", OffsetDateTime.now().toString()));
    }

    public void publishOfferUpdate() {
        CustomerRealtimeEvent event = new CustomerRealtimeEvent(
            "offer-update",
            null,
            null,
            "Offer and banner settings were updated",
            OffsetDateTime.now().toString()
        );

        for (UUID customerId : customerEmitters.keySet()) {
            publishToCustomer(customerId, event);
        }
    }

    private void publishToCustomer(UUID customerId, CustomerRealtimeEvent event) {
        List<SseEmitter> emitters = customerEmitters.get(customerId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            sendPayload(customerId, emitter, event);
        }
    }

    private void sendPayload(UUID customerId, SseEmitter emitter, CustomerRealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (IOException ex) {
            removeEmitter(customerId, emitter);
        }
    }

    private void removeEmitter(UUID customerId, SseEmitter emitter) {
        List<SseEmitter> emitters = customerEmitters.get(customerId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            customerEmitters.remove(customerId);
        }
    }

    private record CustomerRealtimeEvent(
        String type,
        UUID orderId,
        String status,
        String message,
        String timestamp
    ) {
    }
}
