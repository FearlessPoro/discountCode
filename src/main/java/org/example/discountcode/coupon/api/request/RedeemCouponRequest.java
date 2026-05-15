package org.example.discountcode.coupon.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedeemCouponRequest(
        @NotNull @Size(min = 1, max = 100) String userId
) {
}
