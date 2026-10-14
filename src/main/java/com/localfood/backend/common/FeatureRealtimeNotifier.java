package com.localfood.backend.common;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.localfood.backend.customer.CustomerPushNotificationService;
import com.localfood.backend.customer.CustomerRealtimeNotifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class FeatureRealtimeNotifier {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final CustomerRealtimeNotifier customerRealtimeNotifier;
    private final CustomerPushNotificationService customerPushNotificationService;

    public FeatureRealtimeNotifier(
        CustomerRealtimeNotifier customerRealtimeNotifier,
        CustomerPushNotificationService customerPushNotificationService
    ) {
        this.customerRealtimeNotifier = customerRealtimeNotifier;
        this.customerPushNotificationService = customerPushNotificationService;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("features-stream"));
        } catch (IOException ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void publishFeaturesUpdated() {
        afterCommit(() -> {
            sendFeatureMarker();
            customerRealtimeNotifier.publishOfferUpdate();
            customerPushNotificationService.sendOfferBroadcast("Offers or banner were updated. Open app to view latest deals.");
        });
    }

    public void publishAvailabilityUpdated() {
        afterCommit(this::sendFeatureMarker);
    }

    private void afterCommit(Runnable action) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { action.run(); }
                });
        } else action.run();
    }

    private void sendFeatureMarker() {
        String marker = OffsetDateTime.now().toString();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(marker));
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }
}
