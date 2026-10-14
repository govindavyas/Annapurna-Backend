package com.localfood.backend.customer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/customers/me")
public class CustomerProfileController {
    private final CustomerProfileService profiles;
    public CustomerProfileController(CustomerProfileService profiles) { this.profiles = profiles; }
    @GetMapping public CustomerProfileResponse get(@RequestHeader("X-Customer-Token") String token) { return profiles.get(token); }
    @PutMapping public CustomerProfileResponse update(@RequestHeader("X-Customer-Token") String token, @Valid @RequestBody CustomerProfileRequest request) { return profiles.update(token, request); }
    @PutMapping("/address") public CustomerProfileResponse address(@RequestHeader("X-Customer-Token") String token, @Valid @RequestBody CustomerAddressRequest request) { return profiles.address(token, request); }
}
