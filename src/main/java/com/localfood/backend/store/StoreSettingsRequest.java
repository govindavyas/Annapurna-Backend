package com.localfood.backend.store;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreSettingsRequest(
    @NotBlank @Size(max = 120) String shopName,
    @NotNull Boolean shopOpen,
    @NotNull @Min(1) @Max(30) Integer deliveryRadiusKm,
    @Min(-90) @Max(90) Double shopLatitude,
    @Min(-180) @Max(180) Double shopLongitude,
    @NotBlank @Size(max = 120) String bannerTitle,
    @NotBlank @Size(max = 180) String bannerSubtitle,
    @Size(max = 500) String bannerImageUrl,
    @NotNull Boolean offerEnabled,
    @Size(max = 100) String offerTitle,
    @Size(max = 220) String offerDescription,
    @Size(max = 40) String offerCode,
    @Min(0) @Max(10000000) Integer deliveryFeePaise,
    @Min(0) @Max(10000000) Integer minimumOrderPaise
) {
}
