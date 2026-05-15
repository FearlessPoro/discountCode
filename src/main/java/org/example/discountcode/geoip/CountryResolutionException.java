package org.example.discountcode.geoip;

public class CountryResolutionException extends RuntimeException {

    private final Reason reason;

    private CountryResolutionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private CountryResolutionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public static CountryResolutionException countryNotVerified(String message) {
        return new CountryResolutionException(Reason.COUNTRY_NOT_VERIFIED, message);
    }

    public static CountryResolutionException dependencyUnavailable(String message, Throwable cause) {
        return new CountryResolutionException(Reason.DEPENDENCY_UNAVAILABLE, message, cause);
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        COUNTRY_NOT_VERIFIED,
        DEPENDENCY_UNAVAILABLE
    }
}
