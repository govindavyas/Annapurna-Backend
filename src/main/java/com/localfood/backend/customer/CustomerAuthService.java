package com.localfood.backend.customer;

import com.localfood.backend.common.ApiException;
import com.localfood.backend.config.CustomerAuthProperties;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAuthService {

    private static final String HEADER = "X-Customer-Token";

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerSessionRepository customerSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerAuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerAuthService(
        CustomerAccountRepository customerAccountRepository,
        CustomerSessionRepository customerSessionRepository,
        PasswordEncoder passwordEncoder,
        CustomerAuthProperties authProperties
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.customerSessionRepository = customerSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public CustomerSessionResponse register(CustomerRegisterRequest request) {
        String phone = normalizePhone(request.phone());
        if (customerAccountRepository.findByPhone(phone).isPresent()) {
            throw new ApiException("Phone is already registered");
        }

        CustomerAccount account = new CustomerAccount();
        account.setId(UUID.randomUUID());
        account.setName(request.name().trim());
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        customerAccountRepository.save(account);

        return createSession(account);
    }

    @Transactional
    public CustomerSessionResponse login(CustomerLoginRequest request) {
        String phone = normalizePhone(request.phone());
        CustomerAccount account = customerAccountRepository.findByPhone(phone)
            .orElseThrow(() -> new ApiException("Invalid phone or password"));

        if (!passwordEncoder.matches(request.password().trim(), account.getPasswordHash())) {
            throw new ApiException("Invalid phone or password");
        }

        return createSession(account);
    }

    @Transactional(readOnly = true)
    public CustomerAccount requireCustomer(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException("Missing customer token");
        }

        CustomerSession session = customerSessionRepository.findByToken(token.trim())
            .orElseThrow(() -> new ApiException("Invalid session"));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException("Session expired. Please log in again");
        }

        return session.getCustomer();
    }

    @Transactional
    public CustomerLogoutResponse logout(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException("Missing customer token");
        }

        customerSessionRepository.deleteById(token.trim());
        return new CustomerLogoutResponse("Logged out successfully");
    }

    public String tokenHeaderName() {
        return HEADER;
    }

    private CustomerSessionResponse createSession(CustomerAccount account) {
        CustomerSession session = new CustomerSession();
        session.setToken(generateToken());
        session.setCustomer(account);
        session.setCreatedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusDays(authProperties.sessionDays()));
        customerSessionRepository.save(session);

        return new CustomerSessionResponse(
            session.getToken(),
            session.getExpiresAt(),
            account.getName(),
            account.getPhone()
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizePhone(String input) {
        String digits = input == null ? "" : input.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }

        if (digits.length() != 10) {
            throw new ApiException("Phone number must have exactly 10 digits (India format)");
        }

        return digits;
    }
}
