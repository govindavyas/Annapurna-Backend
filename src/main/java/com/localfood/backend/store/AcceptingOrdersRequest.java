package com.localfood.backend.store;

import jakarta.validation.constraints.NotNull;

public record AcceptingOrdersRequest(@NotNull Boolean acceptingOrders) {}
