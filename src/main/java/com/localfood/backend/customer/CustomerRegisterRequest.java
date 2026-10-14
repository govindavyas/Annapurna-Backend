package com.localfood.backend.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
    @NotBlank @Size(min = 2, max = 120) String name,
    @NotBlank String phone,
    @NotBlank @Size(min = 6, max = 120) String password
) {
}
