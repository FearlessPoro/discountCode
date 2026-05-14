package org.example.discountcode.coupon.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCouponRequest(
        @NotNull String code,
        @NotNull @Positive Integer maxUses,
        @NotNull String countryCode
) {
}
