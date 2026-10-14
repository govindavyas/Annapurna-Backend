package com.localfood.backend.delivery;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/delivery-payment")
public class AdminDeliveryPaymentController {
    private final DeliveryPaymentService service;

    public AdminDeliveryPaymentController(DeliveryPaymentService service) {
        this.service = service;
    }

    @GetMapping
    public DeliveryPaymentSettings get(@RequestParam(required = false) UUID branchId) {
        return service.get(branchId);
    }

    @PostMapping(value = "/qr", consumes = "multipart/form-data")
    public DeliveryPaymentSettings upload(@RequestParam(required = false) UUID branchId,
        @RequestParam("file") MultipartFile file) {
        return service.upload(branchId, file);
    }

    @DeleteMapping("/qr")
    public DeliveryPaymentSettings remove(@RequestParam(required = false) UUID branchId) {
        return service.remove(branchId);
    }
}
