package com.localfood.backend.customer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.localfood.backend.config.PushProperties;
import com.localfood.backend.order.OrderStatus;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomerPushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerPushNotificationService.class);

    private final PushProperties pushProperties;
    private final CustomerPushSubscriptionService customerPushSubscriptionService;

    private FirebaseMessaging firebaseMessaging;

    public CustomerPushNotificationService(PushProperties pushProperties, CustomerPushSubscriptionService customerPushSubscriptionService) {
        this.pushProperties = pushProperties;
        this.customerPushSubscriptionService = customerPushSubscriptionService;
    }

    @PostConstruct
    public void init() {
        if (!pushProperties.enabled()) {
            LOGGER.info("Push notifications are disabled (app.push.enabled=false)");
            return;
        }

        String encoded = pushProperties.fcm() == null ? "" : pushProperties.fcm().serviceAccountJsonBase64();
        if (encoded == null || encoded.isBlank()) {
            LOGGER.warn("Push enabled but APP_PUSH_FCM_SERVICE_ACCOUNT_JSON_BASE64 is empty; push delivery will be skipped");
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            this.firebaseMessaging = FirebaseMessaging.getInstance(app);
            LOGGER.info("Firebase push messaging initialized successfully");
        } catch (IllegalArgumentException | IOException ex) {
            LOGGER.error("Failed to initialize Firebase push messaging: {}", ex.getMessage());
        }
    }

    public void sendOrderStatusUpdate(UUID customerId, UUID orderId, OrderStatus status, String message) {
        if (firebaseMessaging == null || customerId == null) {
            return;
        }

        List<CustomerPushSubscription> subscriptions = customerPushSubscriptionService.listActiveByCustomer(customerId);
        if (subscriptions.isEmpty()) {
            return;
        }

        for (CustomerPushSubscription subscription : subscriptions) {
            Message request = Message.builder()
                .setToken(subscription.getToken())
                .setNotification(Notification.builder()
                    .setTitle("Order Update")
                    .setBody(message)
                    .build())
                .putAllData(Map.of(
                    "type", "order-update",
                    "orderId", orderId == null ? "" : orderId.toString(),
                    "status", status == null ? "" : status.name(),
                    "message", message
                ))
                .putData("androidChannelId", pushProperties.fcm() == null ? "orders-updates" : pushProperties.fcm().androidChannelId())
                .build();

            sendSingle(subscription.getToken(), request);
        }
    }

    public void sendOfferBroadcast(String message) {
        if (firebaseMessaging == null) {
            return;
        }

        List<CustomerPushSubscription> subscriptions = customerPushSubscriptionService.listAllActive();
        if (subscriptions.isEmpty()) {
            return;
        }

        for (CustomerPushSubscription subscription : subscriptions) {
            Message request = Message.builder()
                .setToken(subscription.getToken())
                .setNotification(Notification.builder()
                    .setTitle("New Offer Available")
                    .setBody(message)
                    .build())
                .putAllData(Map.of(
                    "type", "offer-update",
                    "message", message
                ))
                .putData("androidChannelId", pushProperties.fcm() == null ? "orders-updates" : pushProperties.fcm().androidChannelId())
                .build();

            sendSingle(subscription.getToken(), request);
        }
    }

    private void sendSingle(String token, Message request) {
        try {
            firebaseMessaging.send(request);
        } catch (FirebaseMessagingException ex) {
            LOGGER.warn("FCM send failed for token {}: {}", token, ex.getMessage());
            customerPushSubscriptionService.markInactiveByToken(token);
        }
    }
}
