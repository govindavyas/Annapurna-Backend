package com.localfood.backend.store;

import com.localfood.backend.admin.AdminAuthContext;
import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreSettingsService {

    private final BranchService branchService;
    private final AdminAuthContext adminAuthContext;

    public StoreSettingsService(BranchService branchService, AdminAuthContext adminAuthContext) {
        this.branchService = branchService;
        this.adminAuthContext = adminAuthContext;
    }

    @Transactional(readOnly = true)
    public StoreSettingsResponse getResponse(java.util.UUID branchId) {
        return StoreSettingsResponse.from(resolveScopedBranch(branchId));
    }

    @Transactional
    public StoreSettingsResponse update(java.util.UUID branchId, StoreSettingsRequest request) {
        Branch branch = branchService.requireForUpdate(resolveScopedBranch(branchId).getId());

        if ((request.shopLatitude() == null) != (request.shopLongitude() == null)) {
            throw new ApiException("Shop latitude and longitude must both be provided");
        }

        if (Boolean.TRUE.equals(request.offerEnabled())) {
            if (request.offerTitle() == null || request.offerTitle().isBlank()) {
                throw new ApiException("Offer title is required when offer is enabled");
            }
            if (request.offerDescription() == null || request.offerDescription().isBlank()) {
                throw new ApiException("Offer description is required when offer is enabled");
            }
        }

        branch.setName(request.shopName().trim());
        branch.setDeliveryRadiusKm(request.deliveryRadiusKm());
        branch.setShopLatitude(request.shopLatitude());
        branch.setShopLongitude(request.shopLongitude());
        branch.setBannerTitle(request.bannerTitle().trim());
        branch.setBannerSubtitle(request.bannerSubtitle().trim());
        branch.setBannerImageUrl(normalizeOptional(request.bannerImageUrl()));
        branch.setOfferEnabled(Boolean.TRUE.equals(request.offerEnabled()));
        branch.setOfferTitle(normalizeOptional(request.offerTitle()));
        branch.setOfferDescription(normalizeOptional(request.offerDescription()));
        branch.setOfferCode(normalizeOptional(request.offerCode()));
        com.localfood.backend.branch.BranchResponse updated = branchService.update(branch.getId(), toBranchRequest(branch));
        // update() refreshes the locked entity; apply optional pricing afterwards.
        if (request.deliveryFeePaise() != null) branch.setDeliveryFeePaise(request.deliveryFeePaise());
        if (request.minimumOrderPaise() != null) branch.setMinimumOrderPaise(request.minimumOrderPaise());
        return StoreSettingsResponse.from(branchService.requireById(updated.id()));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Branch resolveScopedBranch(java.util.UUID requestedBranchId) {
        java.util.UUID scopedBranchId = adminAuthContext.requireBranchIdForScopedAdmin();
        if (scopedBranchId != null) {
            if (requestedBranchId != null && !requestedBranchId.equals(scopedBranchId)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own branch settings");
            }
            return branchService.requireById(scopedBranchId);
        }

        if (requestedBranchId != null) {
            return branchService.requireById(requestedBranchId);
        }

        Branch branch = branchService.resolveBranchForFeatures(null, null, null);
        if (branch == null) throw new ApiException("Create a branch first");
        return branch;
    }

    @Transactional
    public StoreSettingsResponse setAcceptingOrders(java.util.UUID branchId, boolean accepting) {
        return StoreSettingsResponse.from(branchService.setAcceptingOrders(resolveScopedBranch(branchId).getId(), accepting));
    }

    private com.localfood.backend.branch.BranchRequest toBranchRequest(Branch branch) {
        return new com.localfood.backend.branch.BranchRequest(
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
            branch.getOfferCode()
        );
    }
}
