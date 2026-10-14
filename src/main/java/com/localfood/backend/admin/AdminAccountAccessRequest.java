package com.localfood.backend.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminAccountAccessRequest(@NotNull AdminRole role, UUID branchId) {
}
