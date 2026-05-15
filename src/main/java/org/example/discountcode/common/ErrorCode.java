package org.example.discountcode.common;

public enum ErrorCode {
    INVALID_REQUEST("Invalid request."),
    COUPON_NOT_FOUND("Coupon not found."),
    DUPLICATE_COUPON_CODE("Coupon code already exists."),
    COUNTRY_NOT_VERIFIED("Country could not be verified."),
    COUPON_COUNTRY_MISMATCH("Coupon cannot be redeemed from this country."),
    GEOIP_DEPENDENCY_UNAVAILABLE("Country verification service is unavailable."),
    INTERNAL_ERROR("Unexpected error.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
