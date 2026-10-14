package com.localfood.backend.common;

public final class Coordinates {
    private Coordinates() {}
    public static void validate(Double latitude, Double longitude, boolean required) {
        if (latitude == null && longitude == null && !required) return;
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
            || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new ApiException("Choose a valid latitude and longitude together");
        }
    }
}
