package com.localfood.backend.branch;

import com.localfood.backend.admin.AdminAuthContext;
import com.localfood.backend.common.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/branches")
public class AdminBranchController {

    private final BranchService branchService;
    private final AdminAuthContext adminAuthContext;

    public AdminBranchController(BranchService branchService, AdminAuthContext adminAuthContext) {
        this.branchService = branchService;
        this.adminAuthContext = adminAuthContext;
    }

    @GetMapping
    public List<BranchResponse> list() {
        if (!adminAuthContext.isSuperAdmin()) {
            return List.of(BranchResponse.from(branchService.requireById(adminAuthContext.requireBranchIdForScopedAdmin())));
        }
        return branchService.listAll();
    }

    @PostMapping
    public BranchResponse create(@Valid @RequestBody BranchRequest request) {
        ensureSuperAdmin();
        return branchService.create(request);
    }

    @PutMapping("/{branchId}")
    public BranchResponse update(@PathVariable UUID branchId, @Valid @RequestBody BranchRequest request) {
        if (!adminAuthContext.isSuperAdmin()) {
            UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
            if (!branchId.equals(scopedBranchId)) {
                throw new ApiException("Branch admins can only update their own branch");
            }
        }
        return branchService.update(branchId, request);
    }

    private void ensureSuperAdmin() {
        if (!adminAuthContext.isSuperAdmin()) {
            throw new ApiException("Only main admin can manage branches");
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{branchId}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID branchId) {
        ensureSuperAdmin();
        branchService.delete(branchId, adminAuthContext.requireAdmin());
    }
}
