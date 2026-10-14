package com.localfood.backend.menu;

import java.util.UUID;

public record MenuItemResponse(
    UUID id,
    String category,
    String name,
    String description,
    Integer pricePaise,
    boolean available,
    String imageUrl,
    boolean allBranches,
    java.util.Set<UUID> branchIds
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
            item.getId(),
            item.getCategory(),
            item.getName(),
            item.getDescription(),
            item.getPricePaise(),
            item.isAvailable(),
            item.getImageUrl(),
            item.isAllBranches(),
            java.util.Set.copyOf(item.getBranchIds())
        );
    }
}
