package com.localfood.backend.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, String> {

    Optional<CustomerSession> findByToken(String token);
}
