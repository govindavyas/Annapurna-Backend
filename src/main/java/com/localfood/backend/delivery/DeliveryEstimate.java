package com.localfood.backend.delivery;
import com.localfood.backend.order.CustomerOrder;
public final class DeliveryEstimate {
    private DeliveryEstimate() {}
    public static void calculate(CustomerOrder order) {
        if (order.getDeliveryLatitude() == null || order.getDeliveryLongitude() == null) return;
        double latitude = order.getRiderLatitude() == null ? order.getDispatchLatitude() : order.getRiderLatitude();
        double longitude = order.getRiderLongitude() == null ? order.getDispatchLongitude() : order.getRiderLongitude();
        var observedAt = order.getRiderLocationAt() == null ? order.getDispatchedAt() : order.getRiderLocationAt();
        double lat1 = Math.toRadians(latitude), lat2 = Math.toRadians(order.getDeliveryLatitude());
        double dLat = lat2 - lat1, dLon = Math.toRadians(order.getDeliveryLongitude() - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        double directKm = 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
        // Approximation, not a road/traffic quote: 35% road allowance, 20 km/h and doorstep buffer.
        double roadKm = directKm * 1.35;
        int minutes = Math.max(5, (int)Math.ceil(roadKm / 20 * 60) + 3);
        order.setEstimatedDistanceKm(Math.round(roadKm * 10) / 10.0);
        order.setEstimatedArrivalFrom(observedAt.plusMinutes(minutes));
        order.setEstimatedArrivalTo(observedAt.plusMinutes(minutes + 10));
    }
}
