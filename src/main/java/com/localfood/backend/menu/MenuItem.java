package com.localfood.backend.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "price_paise", nullable = false)
    private Integer pricePaise;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "all_branches", nullable = false)
    private boolean allBranches = true;

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "menu_item_branches", joinColumns = @jakarta.persistence.JoinColumn(name = "menu_item_id"))
    @Column(name = "branch_id", nullable = false)
    private java.util.Set<UUID> branchIds = new java.util.HashSet<>();

    public boolean isServedBy(UUID branchId) {
        return allBranches || branchIds.contains(branchId);
    }
}
