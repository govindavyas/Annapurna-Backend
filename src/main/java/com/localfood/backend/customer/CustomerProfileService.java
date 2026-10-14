package com.localfood.backend.customer;

import com.localfood.backend.common.ApiException;
import com.localfood.backend.common.Coordinates;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {
    private final CustomerAuthService auth;
    private final CustomerAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final EntityManager entities;
    public CustomerProfileService(CustomerAuthService auth, CustomerAccountRepository accounts,
        PasswordEncoder passwords, EntityManager entities) {
        this.auth = auth; this.accounts = accounts; this.passwords = passwords; this.entities = entities;
    }
    @Transactional(readOnly=true)
    public CustomerProfileResponse get(String token) { return CustomerProfileResponse.from(auth.requireCustomer(token)); }
    private CustomerAccount locked(String token) {
        CustomerAccount account = auth.requireCustomer(token);
        entities.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }
    @Transactional
    public CustomerProfileResponse update(String token, CustomerProfileRequest request) {
        CustomerAccount account = locked(token);
        if (!request.phone().equals(account.getPhone())) {
            if (request.currentPassword() == null || !passwords.matches(request.currentPassword().trim(), account.getPasswordHash())) {
                throw new ApiException("Confirm your current password to change the login phone number");
            }
            if (accounts.findByPhone(request.phone()).isPresent()) throw new ApiException("Phone is already registered");
            account.setPhone(request.phone());
        }
        account.setName(request.name().trim());
        try { accounts.saveAndFlush(account); }
        catch (org.springframework.dao.DataIntegrityViolationException ex) { throw new ApiException("Phone is already registered"); }
        return CustomerProfileResponse.from(account);
    }
    @Transactional
    public CustomerProfileResponse address(String token, CustomerAddressRequest request) {
        Coordinates.validate(request.latitude(), request.longitude(), false);
        CustomerAccount account = locked(token);
        account.setDeliveryAddress(request.address().trim());
        account.setDeliveryNotes(request.notes() == null ? null : request.notes().trim());
        account.setDeliveryLatitude(request.latitude()); account.setDeliveryLongitude(request.longitude());
        return CustomerProfileResponse.from(accounts.save(account));
    }
}
