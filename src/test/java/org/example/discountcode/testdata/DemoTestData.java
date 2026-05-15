package org.example.discountcode.testdata;

import java.time.Instant;
import org.example.discountcode.coupon.domain.Coupon;

public final class DemoTestData {

    public static final String PLTEST_CODE = "PLTEST";
    public static final String USTEST_CODE = "USTEST";
    public static final String LIMIT1_CODE = "LIMIT1";

    public static final String PL_COUNTRY = "PL";
    public static final String US_COUNTRY = "US";

    public static final String USER_1 = "user-1";
    public static final String USER_2 = "user-2";
    public static final String USER_3 = "user-3";

    private DemoTestData() {
    }

    public static Coupon plTestCoupon(Instant createdAt) {
        return new Coupon(PLTEST_CODE, 2, PL_COUNTRY, createdAt);
    }

    public static Coupon usTestCoupon(Instant createdAt) {
        return new Coupon(USTEST_CODE, 1, US_COUNTRY, createdAt);
    }

    public static Coupon limitOneCoupon(Instant createdAt) {
        return new Coupon(LIMIT1_CODE, 1, US_COUNTRY, createdAt);
    }
}
