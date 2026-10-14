package com.localfood.backend.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    boolean existsByBranchIdAndStatusIn(UUID branchId, java.util.Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findForUpdate(@Param("id") UUID id);

    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<CustomerOrder> findByBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, OrderStatus status);
    List<CustomerOrder> findByBranchIdAndStatusInOrderByCreatedAtDesc(UUID branchId, java.util.Collection<OrderStatus> statuses);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
