package com.localfood.backend.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    long countByCategory(String category);
    java.util.List<MenuItem> findByCategory(String category);
    java.util.Optional<MenuItem> findFirstByCategoryIgnoreCase(String category);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "branchIds")
    List<MenuItem> findByAvailableTrueOrderByCategoryAscNameAsc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "branchIds")
    List<MenuItem> findAllByOrderByCategoryAscNameAsc();
}
