package org.example.discountcode.coupon.application;

import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.geoip.CountryResolutionException;
import org.example.discountcode.geoip.CountryResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CouponCountryRestrictionService {

    private final CountryResolver countryResolver;

    public CouponCountryRestrictionService(CountryResolver countryResolver) {
        this.countryResolver = countryResolver;
    }

    public String verifyCouponCountry(Coupon coupon, String ipAddress) {
        String resolvedCountryCode;
        try {
            resolvedCountryCode = countryResolver.resolveCountryCode(ipAddress);
        } catch (CountryResolutionException exception) {
            if (exception.reason() == CountryResolutionException.Reason.DEPENDENCY_UNAVAILABLE) {
                throw new BusinessException(ErrorCode.GEOIP_DEPENDENCY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, exception);
            }
            throw new BusinessException(ErrorCode.COUNTRY_NOT_VERIFIED, HttpStatus.FORBIDDEN, exception);
        }

        if (!coupon.countryCode().equals(resolvedCountryCode)) {
            throw new BusinessException(ErrorCode.COUPON_COUNTRY_MISMATCH, HttpStatus.FORBIDDEN);
        }
        return resolvedCountryCode;
    }
}
