package com.localfood.backend.order;

import com.localfood.backend.branch.Branch;
import com.localfood.backend.branch.BranchService;
import com.localfood.backend.common.FeatureRealtimeNotifier;
import com.localfood.backend.config.FeatureProperties;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureController {

    private final FeatureProperties featureProperties;
    private final BranchService branchService;
    private final FeatureRealtimeNotifier featureRealtimeNotifier;

    public FeatureController(
        FeatureProperties featureProperties,
        BranchService branchService,
        FeatureRealtimeNotifier featureRealtimeNotifier
    ) {
        this.featureProperties = featureProperties;
        this.branchService = branchService;
        this.featureRealtimeNotifier = featureRealtimeNotifier;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter featureStream() {
        return featureRealtimeNotifier.subscribe();
    }

    @GetMapping
    public FeatureStatusResponse featureStatus(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) Double deliveryLatitude,
        @RequestParam(required = false) Double deliveryLongitude
    ) {
        Branch branch = branchService.resolveBranchForFeatures(branchId, deliveryLatitude, deliveryLongitude);
        if (branch == null) {
            return new FeatureStatusResponse(featureProperties.codEnabled(), featureProperties.paymentsEnabled(),
                featureProperties.smsOtpEnabled(), featureProperties.whatsappEnabled(), null, null, null,
                false, 0, "Annapurna", "Our kitchens are currently closed", "Please check back later.",
                null, false, null, null, null, java.util.List.of(), 0, 0, false, "No branch is available");
        }
        return new FeatureStatusResponse(
            featureProperties.codEnabled(),
            featureProperties.paymentsEnabled(),
            featureProperties.smsOtpEnabled(),
            featureProperties.whatsappEnabled(),
            branch.getId(),
            branch.getCode(),
            branch.getName(),
            branch.isShopOpen(),
            branch.getDeliveryRadiusKm(),
            branch.getName(),
            branch.getBannerTitle(),
            branch.getBannerSubtitle(),
            branch.getBannerImageUrl(),
            branch.isOfferEnabled(),
            branch.getOfferTitle(),
            branch.getOfferDescription(),
            branch.getOfferCode(),
            branchService.listPublic(),
            branch.getDeliveryFeePaise(),
            branch.getMinimumOrderPaise(),
            branchService.deliveryUnavailableReason(branch, deliveryLatitude, deliveryLongitude) == null,
            branchService.deliveryUnavailableReason(branch, deliveryLatitude, deliveryLongitude)
        );
    }
}
