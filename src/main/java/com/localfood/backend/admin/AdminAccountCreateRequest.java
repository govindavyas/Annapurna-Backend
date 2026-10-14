package com.localfood.backend.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record AdminAccountCreateRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,80}", message = "Use 3–80 letters, numbers, dots, underscores or hyphens") String username,
    @NotBlank @Size(min = 8, max = 72, message = "Password must be 8–72 characters (at most 72 UTF-8 bytes)") String password,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{6,40}", message = "Access code must be 6–40 letters, numbers, underscores or hyphens") String totpSecret,
    @NotNull AdminRole role,
    UUID branchId
) {
}
