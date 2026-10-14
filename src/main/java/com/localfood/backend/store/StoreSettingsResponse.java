package com.localfood.backend.store;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreSettingsResponse(
    UUID branchId,
    String branchCode,
    String shopName,
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
    OffsetDateTime updatedAt,
    int deliveryFeePaise,
    int minimumOrderPaise
) {
    public static StoreSettingsResponse from(com.localfood.backend.branch.Branch branch) {
        return new StoreSettingsResponse(
            branch.getId(),
            branch.getCode(),
            branch.getName(),
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
            branch.getUpdatedAt(),
            branch.getDeliveryFeePaise(),
            branch.getMinimumOrderPaise()
        );
    }
}
