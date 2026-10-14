package com.localfood.backend.customer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/register")
    public CustomerSessionResponse register(@Valid @RequestBody CustomerRegisterRequest request) {
        return customerAuthService.register(request);
    }

    @PostMapping("/login")
    public CustomerSessionResponse login(@Valid @RequestBody CustomerLoginRequest request) {
        return customerAuthService.login(request);
    }

    @DeleteMapping("/session")
    public CustomerLogoutResponse logout(@RequestHeader("X-Customer-Token") String token) {
        return customerAuthService.logout(token);
    }
}
