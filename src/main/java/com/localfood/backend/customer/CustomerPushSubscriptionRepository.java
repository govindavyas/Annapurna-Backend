package com.localfood.backend.customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPushSubscriptionRepository extends JpaRepository<CustomerPushSubscription, Long> {

    Optional<CustomerPushSubscription> findByToken(String token);

    List<CustomerPushSubscription> findByCustomerIdAndActiveTrue(UUID customerId);

    List<CustomerPushSubscription> findByActiveTrue();
}
