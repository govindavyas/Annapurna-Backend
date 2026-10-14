package com.localfood.backend.customer;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerPushSubscriptionService {

    private final CustomerPushSubscriptionRepository repository;

    public CustomerPushSubscriptionService(CustomerPushSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerPushSubscriptionResponse register(UUID customerId, CustomerPushSubscriptionRequest request) {
        String token = request.token().trim();
        CustomerPushSubscription subscription = repository.findByToken(token)
            .orElseGet(CustomerPushSubscription::new);

        subscription.setCustomerId(customerId);
        subscription.setPlatform(request.platform());
        subscription.setToken(token);
        subscription.setActive(true);
        repository.save(subscription);

        return new CustomerPushSubscriptionResponse(subscription.getPlatform(), subscription.getToken(), subscription.isActive(), "Push subscription saved");
    }

    @Transactional
    public CustomerPushSubscriptionResponse unregister(UUID customerId, String rawToken) {
        String token = rawToken.trim();
        CustomerPushSubscription subscription = repository.findByToken(token)
            .filter(row -> row.getCustomerId().equals(customerId))
            .orElse(null);

        if (subscription == null) {
            return new CustomerPushSubscriptionResponse(PushPlatform.WEB, token, false, "Push subscription not found");
        }

        subscription.setActive(false);
        repository.save(subscription);

        return new CustomerPushSubscriptionResponse(subscription.getPlatform(), subscription.getToken(), false, "Push subscription removed");
    }

    @Transactional(readOnly = true)
    public List<CustomerPushSubscription> listActiveByCustomer(UUID customerId) {
        return repository.findByCustomerIdAndActiveTrue(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerPushSubscription> listAllActive() {
        return repository.findByActiveTrue();
    }

    @Transactional
    public void markInactiveByToken(String token) {
        repository.findByToken(token)
            .ifPresent(subscription -> {
                subscription.setActive(false);
                repository.save(subscription);
            });
    }
}
