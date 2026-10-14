package com.localfood.backend.store;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminStoreController {

    private final StoreSettingsService storeSettingsService;

    public AdminStoreController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public StoreSettingsResponse getSettings(@RequestParam(required = false) UUID branchId) {
        return storeSettingsService.getResponse(branchId);
    }

    @PutMapping
    public StoreSettingsResponse updateSettings(
        @RequestParam(required = false) UUID branchId,
        @Valid @RequestBody StoreSettingsRequest request
    ) {
        return storeSettingsService.update(branchId, request);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/accepting-orders")
    public StoreSettingsResponse acceptingOrders(@RequestParam(required = false) UUID branchId,
        @Valid @RequestBody AcceptingOrdersRequest request) {
        return storeSettingsService.setAcceptingOrders(branchId, request.acceptingOrders());
    }
}
