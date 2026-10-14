package com.localfood.backend.branch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Branch b where b.id = :id")
    Optional<Branch> findForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<Branch> findByCodeIgnoreCase(String code);

    List<Branch> findByShopOpenTrueAndDeletedFalseOrderByNameAsc();

    List<Branch> findByDeletedFalseOrderByNameAsc();
}
