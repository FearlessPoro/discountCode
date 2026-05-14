package org.example.discountcode.coupon.domain;

import java.util.Locale;

public final class CouponCode {

    private static final int MAX_LENGTH = 50;
    private static final String CODE_PATTERN = "[A-Za-z0-9]+";

    private CouponCode() {
    }

    public static String normalizeForCreation(String rawCode) {
        if (!isValid(rawCode)) {
            throw new IllegalArgumentException("Coupon code must be 1-50 ASCII letters or digits.");
        }
        return rawCode.toUpperCase(Locale.ROOT);
    }

    private static boolean isValid(String code) {
        return code != null
                && !code.isEmpty()
                && code.length() <= MAX_LENGTH
                && code.matches(CODE_PATTERN);
    }
}
