package com.localfood.backend.delivery;

import java.util.UUID;

public record DeliveryPaymentSettings(UUID branchId, String branchName, String qrImageUrl) {
}
