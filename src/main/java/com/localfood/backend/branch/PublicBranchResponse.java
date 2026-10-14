package com.localfood.backend.branch;

import java.util.UUID;

public record PublicBranchResponse(
    UUID id,
    String code,
    String name,
    String address,
    boolean shopOpen,
    Integer deliveryRadiusKm,
    Double shopLatitude,
    Double shopLongitude,
    int deliveryFeePaise,
    int minimumOrderPaise
) {
    public static PublicBranchResponse from(Branch branch) {
        return new PublicBranchResponse(
            branch.getId(),
            branch.getCode(),
            branch.getName(),
            branch.getAddress(),
            branch.isShopOpen(),
            branch.getDeliveryRadiusKm(),
            branch.getShopLatitude(),
            branch.getShopLongitude(),
            branch.getDeliveryFeePaise(),
            branch.getMinimumOrderPaise()
        );
    }
}
