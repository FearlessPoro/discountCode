package org.example.discountcode.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.geoip.CountryResolutionException;
import org.example.discountcode.geoip.CountryResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CouponCountryRestrictionServiceTest {

    private final CountryResolver countryResolver = org.mockito.Mockito.mock(CountryResolver.class);
    private final CouponCountryRestrictionService service = new CouponCountryRestrictionService(countryResolver);

    @Test
    void acceptsMatchingResolvedCountry() {
        Coupon coupon = new Coupon("SAVE10", 5, "PL", Instant.parse("2026-05-14T12:00:00Z"));
        when(countryResolver.resolveCountryCode("203.0.113.10")).thenReturn("PL");

        assertThat(service.verifyCouponCountry(coupon, "203.0.113.10")).isEqualTo("PL");
    }

    @Test
    void rejectsDifferentResolvedCountry() {
        Coupon coupon = new Coupon("SAVE10", 5, "PL", Instant.parse("2026-05-14T12:00:00Z"));
        when(countryResolver.resolveCountryCode("203.0.113.10")).thenReturn("DE");

        assertThatThrownBy(() -> service.verifyCouponCountry(coupon, "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_COUNTRY_MISMATCH);
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void mapsUnverifiedCountryToForbidden() {
        Coupon coupon = new Coupon("SAVE10", 5, "PL", Instant.parse("2026-05-14T12:00:00Z"));
        when(countryResolver.resolveCountryCode("127.0.0.1"))
                .thenThrow(CountryResolutionException.countryNotVerified("Country could not be verified."));

        assertThatThrownBy(() -> service.verifyCouponCountry(coupon, "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUNTRY_NOT_VERIFIED);
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void mapsUnavailableProviderToServiceUnavailable() {
        Coupon coupon = new Coupon("SAVE10", 5, "PL", Instant.parse("2026-05-14T12:00:00Z"));
        when(countryResolver.resolveCountryCode("203.0.113.10"))
                .thenThrow(CountryResolutionException.dependencyUnavailable("Unavailable.", new RuntimeException()));

        assertThatThrownBy(() -> service.verifyCouponCountry(coupon, "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.GEOIP_DEPENDENCY_UNAVAILABLE);
                    assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }
}
