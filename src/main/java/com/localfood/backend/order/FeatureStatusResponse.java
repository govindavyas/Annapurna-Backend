package com.localfood.backend.order;

import com.localfood.backend.branch.PublicBranchResponse;
import java.util.List;
import java.util.UUID;

public record FeatureStatusResponse(
    boolean codEnabled,
    boolean paymentsEnabled,
    boolean smsOtpEnabled,
    boolean whatsappEnabled,
    UUID selectedBranchId,
    String selectedBranchCode,
    String selectedBranchName,
    boolean shopOpen,
    int deliveryRadiusKm,
    String shopName,
    String bannerTitle,
    String bannerSubtitle,
    String bannerImageUrl,
    boolean offerEnabled,
    String offerTitle,
    String offerDescription,
    String offerCode,
    List<PublicBranchResponse> branches,
    int deliveryFeePaise,
    int minimumOrderPaise,
    boolean deliveryAvailable,
    String deliveryUnavailableReason
) {
}
