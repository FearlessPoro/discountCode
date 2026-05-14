package org.example.discountcode.coupon.domain;

import java.util.Locale;
import java.util.Set;

public final class CountryCode {

    private static final Set<String> ISO_COUNTRY_CODES = Set.copyOf(Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2));

    private CountryCode() {
    }

    public static String normalize(String rawCountryCode) {
        if (rawCountryCode == null || rawCountryCode.isBlank()) {
            throw new IllegalArgumentException("Country code must be ISO 3166-1 alpha-2.");
        }

        String countryCode = rawCountryCode.toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRY_CODES.contains(countryCode)) {
            throw new IllegalArgumentException("Country code must be ISO 3166-1 alpha-2.");
        }
        return countryCode;
    }
}
