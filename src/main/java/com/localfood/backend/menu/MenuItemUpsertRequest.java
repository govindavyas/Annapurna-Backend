package com.localfood.backend.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MenuItemUpsertRequest(
    @NotBlank String category,
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @Min(1) Integer pricePaise,
    @NotNull Boolean available,
    String imageUrl,
    Boolean allBranches,
    java.util.Set<@NotNull UUID> branchIds
) {
}
