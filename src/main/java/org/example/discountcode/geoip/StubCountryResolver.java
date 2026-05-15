package org.example.discountcode.geoip;

import java.util.Map;
import org.example.discountcode.coupon.domain.CountryCode;

public class StubCountryResolver implements CountryResolver {

    private final Map<String, String> countryByIp;
    private final String defaultCountryCode;

    public StubCountryResolver(Map<String, String> countryByIp, String defaultCountryCode) {
        this.countryByIp = Map.copyOf(countryByIp == null ? Map.of() : countryByIp);
        this.defaultCountryCode = defaultCountryCode == null || defaultCountryCode.isBlank()
                ? null
                : CountryCode.normalize(defaultCountryCode);
    }

    @Override
    public String resolveCountryCode(String ipAddress) {
        String configuredCountry = countryByIp.get(ipAddress);
        String countryCode = configuredCountry == null ? defaultCountryCode : configuredCountry;
        if (countryCode == null) {
            throw CountryResolutionException.countryNotVerified("Country could not be verified.");
        }

        try {
            return CountryCode.normalize(countryCode);
        } catch (IllegalArgumentException exception) {
            throw CountryResolutionException.countryNotVerified("Country could not be verified.");
        }
    }
}
