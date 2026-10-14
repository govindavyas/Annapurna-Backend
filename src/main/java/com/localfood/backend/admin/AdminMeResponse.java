package com.localfood.backend.admin;

import java.util.UUID;

public record AdminMeResponse(
    String username,
    AdminRole role,
    UUID branchId,
    UUID id
) {
}
