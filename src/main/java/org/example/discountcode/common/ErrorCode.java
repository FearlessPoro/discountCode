package org.example.discountcode.common;

public enum ErrorCode {
    INVALID_REQUEST("Invalid request."),
    COUPON_NOT_FOUND("Coupon not found."),
    DUPLICATE_COUPON_CODE("Coupon code already exists."),
    INTERNAL_ERROR("Unexpected error.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
