package com.localfood.backend.delivery;
import jakarta.validation.constraints.NotNull;
public record StartDeliveryRequest(@NotNull Double latitude, @NotNull Double longitude, @NotNull LocationSource source) {
    public enum LocationSource { GPS, MAP }
}
