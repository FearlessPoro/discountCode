package org.example.discountcode.coupon.api.response;

import java.time.Instant;

public record CouponResponse(
        String code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        String countryCode
) {
}
