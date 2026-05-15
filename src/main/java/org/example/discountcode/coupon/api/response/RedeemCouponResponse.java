package org.example.discountcode.coupon.api.response;

import java.time.Instant;

public record RedeemCouponResponse(
        String couponCode,
        String userId,
        Instant redeemedAt,
        String status
) {
}
