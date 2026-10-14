package com.localfood.backend.delivery;
import com.localfood.backend.order.CustomerOrder;
import com.localfood.backend.order.OrderStatus;
import java.time.OffsetDateTime;
public record DeliveryEstimateResponse(OffsetDateTime dispatchedAt, Double latitude, Double longitude,
    String locationSource, Double approximateDistanceKm, OffsetDateTime arrivalFrom, OffsetDateTime arrivalTo,
    OffsetDateTime locationUpdatedAt, boolean live, Double accuracyMeters) {
    public static DeliveryEstimateResponse from(CustomerOrder order) {
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY || order.getDispatchedAt() == null) return null;
        boolean fresh = order.isRiderSharing() && order.getRiderLocationAt() != null
            && order.getRiderLocationAt().isAfter(OffsetDateTime.now().minusSeconds(45));
        return new DeliveryEstimateResponse(order.getDispatchedAt(),
            order.getRiderLatitude() == null ? order.getDispatchLatitude() : order.getRiderLatitude(),
            order.getRiderLongitude() == null ? order.getDispatchLongitude() : order.getRiderLongitude(),
            order.getRiderLocationAt() == null ? order.getDispatchLocationSource() : "GPS",
            order.getEstimatedDistanceKm(), order.getEstimatedArrivalFrom(), order.getEstimatedArrivalTo(),
            order.getRiderLocationAt() == null ? order.getDispatchedAt() : order.getRiderLocationAt(), fresh, order.getRiderAccuracyMeters());
    }
}
