package com.localfood.backend.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdminAccount a where a.role = com.localfood.backend.admin.AdminRole.SUPER_ADMIN order by a.id")
    List<AdminAccount> lockMainAdmins();

    Optional<AdminAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRole(AdminRole role);

    long countByRoleAndActiveTrue(AdminRole role);

    List<AdminAccount> findByBranchIdOrderByUsernameAsc(UUID branchId);
}
