package com.localfood.backend.customer;
import jakarta.validation.constraints.*;
public record CustomerProfileRequest(@NotBlank @Size(max=120) String name,
    @NotBlank @Pattern(regexp="[0-9]{10}", message="Enter a 10-digit phone number") String phone,
    @Size(max=120) String currentPassword) {}
