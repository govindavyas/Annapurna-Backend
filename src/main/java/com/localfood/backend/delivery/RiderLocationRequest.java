package com.localfood.backend.delivery;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
public record RiderLocationRequest(@NotNull Double latitude, @NotNull Double longitude,
    @NotNull @DecimalMin("0") @DecimalMax("200") Double accuracyMeters, @NotNull OffsetDateTime observedAt) {}
