package com.localfood.backend.menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, String> {
    List<MenuCategory> findAllByOrderByNameAsc();
    Optional<MenuCategory> findByNormalizedName(String normalizedName);
}
