package com.localfood.backend.customer;

import java.time.OffsetDateTime;

public record CustomerSessionResponse(
    String token,
    OffsetDateTime expiresAt,
    String name,
    String phone
) {
}
