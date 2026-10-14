package com.localfood.backend.admin;

import com.localfood.backend.common.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAccountController {

    private final AdminAuthContext authContext;
    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAuthContext authContext, AdminAccountService adminAccountService) {
        this.authContext = authContext;
        this.adminAccountService = adminAccountService;
    }

    @GetMapping("/me")
    public AdminMeResponse me() {
        return adminAccountService.me(authContext.requireAdmin());
    }

    @GetMapping("/accounts")
    public List<AdminAccountResponse> listAccounts() {
        if (!authContext.isSuperAdmin()) {
            throw new ApiException("Only main admin can view all admin accounts");
        }
        return adminAccountService.listAll();
    }

    @PostMapping("/accounts")
    public AdminAccountResponse createAccount(@Valid @RequestBody AdminAccountCreateRequest request) {
        if (!authContext.isSuperAdmin()) {
            throw new ApiException("Only main admin can create admin accounts");
        }
        return adminAccountService.create(request, authContext.requireAdmin());
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AdminAccountResponse updateAccountStatus(
        @PathVariable UUID accountId,
        @Valid @RequestBody AdminAccountStatusUpdateRequest request
    ) {
        if (!authContext.isSuperAdmin()) {
            throw new ApiException("Only main admin can update admin account status");
        }
        return adminAccountService.updateActive(accountId, request.active(), authContext.requireAdmin());
    }

    @GetMapping("/branches/{branchId}/accounts")
    public List<AdminAccountResponse> listBranchAccounts(@PathVariable UUID branchId) {
        if (!authContext.isSuperAdmin()) {
            UUID scopedBranchId = authContext.requireBranchIdForScopedAdmin();
            if (!branchId.equals(scopedBranchId)) {
                throw new ApiException("Branch admins can only view their own branch accounts");
            }
        }
        return adminAccountService.listByBranch(branchId);
    }

    @PatchMapping("/accounts/{accountId}/access")
    public AdminAccountResponse updateAccess(@PathVariable UUID accountId, @Valid @RequestBody AdminAccountAccessRequest request) {
        return adminAccountService.updateAccess(accountId, request, authContext.requireAdmin());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/accounts/{accountId}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable UUID accountId) {
        adminAccountService.delete(accountId, authContext.requireAdmin());
    }
}
