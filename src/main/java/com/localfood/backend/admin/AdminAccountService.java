package com.localfood.backend.admin;

import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.ApiException;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    private final AdminAccountRepository repository;
    private final BranchService branchService;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AdminAccountRepository repository, BranchService branchService, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.branchService = branchService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> listAll() {
        return repository.findAll(org.springframework.data.domain.Sort.by("username")).stream()
            .filter(account -> !account.isDeleted()).map(AdminAccountResponse::from).toList();
    }

    @Transactional
    public AdminAccountResponse create(AdminAccountCreateRequest request, AdminUserDetails actor) {
        lockAndRequireMainAdmin(actor);
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists. Choose a different username");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("Password must not exceed 72 UTF-8 bytes. Use a shorter password");
        }
        Branch branch = validateAccess(request.role(), request.branchId());

        AdminAccount account = new AdminAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setTotpSecret(request.totpSecret().trim());
        account.setRole(request.role());
        account.setBranch(branch);
        account.setActive(true);

        return AdminAccountResponse.from(repository.save(account));
    }

    @Transactional(readOnly = true)
    public AdminMeResponse me(AdminUserDetails principal) {
        return new AdminMeResponse(principal.getUsername(), principal.getRole(), principal.getBranchId(), principal.getId());
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> listByBranch(UUID branchId) {
        return repository.findByBranchIdOrderByUsernameAsc(branchId).stream()
            .filter(account -> !account.isDeleted()).map(AdminAccountResponse::from).toList();
    }

    @Transactional
    public AdminAccountResponse updateActive(UUID accountId, boolean active, AdminUserDetails actor) {
        lockAndRequireMainAdmin(actor);
        AdminAccount account = repository.findById(accountId)
            .filter(user -> !user.isDeleted())
            .orElseThrow(() -> new ApiException("Admin account not found"));

        if (account.getUsername().equalsIgnoreCase(actor.getUsername()) && !active) {
            throw new ApiException("You cannot deactivate your own account");
        }

        if (!active && account.getRole() == AdminRole.SUPER_ADMIN && account.isActive()) {
            long activeMainAdmins = repository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN);
            if (activeMainAdmins <= 1) {
                throw new ApiException("At least one active main admin is required");
            }
        }

        account.setActive(active);
        return AdminAccountResponse.from(repository.save(account));
    }

    @Transactional
    public AdminAccountResponse updateAccess(UUID accountId, AdminAccountAccessRequest request, AdminUserDetails actor) {
        lockAndRequireMainAdmin(actor);
        AdminAccount account = repository.findById(accountId).filter(user -> !user.isDeleted())
            .orElseThrow(() -> new ApiException("Team account not found"));
        if (account.getId().equals(actor.getId())) {
            throw new ApiException("You cannot change your own role or branch");
        }
        if (account.getRole() == AdminRole.SUPER_ADMIN && account.isActive()
            && request.role() != AdminRole.SUPER_ADMIN && repository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN) <= 1) {
            throw new ApiException("At least one active main admin is required");
        }
        account.setBranch(validateAccess(request.role(), request.branchId()));
        account.setRole(request.role());
        return AdminAccountResponse.from(repository.save(account));
    }

    private Branch validateAccess(AdminRole role, UUID branchId) {
        if (role == AdminRole.SUPER_ADMIN && branchId != null) {
            throw new ApiException("Main admins have access to all branches. Do not assign a single branch");
        }
        if (role != AdminRole.SUPER_ADMIN && branchId == null) {
            throw new ApiException("Select a branch for the branch admin or delivery staff account");
        }
        return branchId == null ? null : branchService.requireForUpdate(branchId);
    }

    private void lockAndRequireMainAdmin(AdminUserDetails actor) {
        // Serialize access changes so concurrent requests cannot remove all main admins.
        boolean allowed = repository.lockMainAdmins().stream()
            .anyMatch(account -> account.getId().equals(actor.getId()) && account.isActive() && !account.isDeleted());
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an active main admin can manage team access");
    }

    @Transactional
    public void delete(UUID accountId, AdminUserDetails actor) {
        lockAndRequireMainAdmin(actor);
        AdminAccount account = repository.findById(accountId).filter(user -> !user.isDeleted())
            .orElseThrow(() -> new ApiException("Team account not found"));
        if (account.getId().equals(actor.getId())) throw new ApiException("You cannot delete your own account");
        if (account.getRole() == AdminRole.SUPER_ADMIN && account.isActive()
            && repository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN) <= 1) {
            throw new ApiException("At least one active main admin is required");
        }
        account.setActive(false);
        account.setDeleted(true);
    }
}
