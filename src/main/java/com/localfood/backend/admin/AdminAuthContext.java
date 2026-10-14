package com.localfood.backend.admin;

import com.localfood.backend.common.ApiException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthContext {

    public AdminUserDetails requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminUserDetails admin)) {
            throw new ApiException("Admin authentication is required");
        }
        return admin;
    }

    public boolean isSuperAdmin() {
        return requireAdmin().getRole() == AdminRole.SUPER_ADMIN;
    }

    public UUID requireBranchIdForScopedAdmin() {
        AdminUserDetails admin = requireAdmin();
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            return null;
        }
        if (admin.getBranchId() == null) {
            throw new ApiException("Branch admin is not linked to a branch");
        }
        return admin.getBranchId();
    }
}
