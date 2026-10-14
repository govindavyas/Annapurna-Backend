package com.localfood.backend.branch;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchResponse(
    UUID id,
    String code,
    String name,
    String address,
    boolean shopOpen,
    int deliveryRadiusKm,
    Double shopLatitude,
    Double shopLongitude,
    String bannerTitle,
    String bannerSubtitle,
    String bannerImageUrl,
    boolean offerEnabled,
    String offerTitle,
    String offerDescription,
    String offerCode,
    OffsetDateTime updatedAt
) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
            branch.getId(),
            branch.getCode(),
            branch.getName(),
            branch.getAddress(),
            branch.isShopOpen(),
            branch.getDeliveryRadiusKm(),
            branch.getShopLatitude(),
            branch.getShopLongitude(),
            branch.getBannerTitle(),
            branch.getBannerSubtitle(),
            branch.getBannerImageUrl(),
            branch.isOfferEnabled(),
            branch.getOfferTitle(),
            branch.getOfferDescription(),
            branch.getOfferCode(),
            branch.getUpdatedAt()
        );
    }
}
