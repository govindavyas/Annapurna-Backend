package com.localfood.backend.admin;

import java.util.UUID;

public record AdminAccountResponse(
    UUID id,
    String username,
    AdminRole role,
    UUID branchId,
    boolean active
) {
    public static AdminAccountResponse from(AdminAccount account) {
        return new AdminAccountResponse(
            account.getId(),
            account.getUsername(),
            account.getRole(),
            account.getBranch() == null ? null : account.getBranch().getId(),
            account.isActive()
        );
    }
}
