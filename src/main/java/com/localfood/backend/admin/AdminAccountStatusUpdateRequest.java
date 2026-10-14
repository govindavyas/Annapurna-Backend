package com.localfood.backend.admin;

import jakarta.validation.constraints.NotNull;

public record AdminAccountStatusUpdateRequest(
    @NotNull Boolean active
) {
}
