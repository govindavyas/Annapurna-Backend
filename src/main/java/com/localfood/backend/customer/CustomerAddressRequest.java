package com.localfood.backend.customer;
import jakarta.validation.constraints.*;
public record CustomerAddressRequest(@NotBlank @Size(max=500) String address,
    @Size(max=300) String notes, Double latitude, Double longitude) {}
