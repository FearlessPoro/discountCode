package org.example.discountcode.coupon.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.api.response.RedeemCouponResponse;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.coupon.domain.CouponUsage;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.example.discountcode.coupon.infrastructure.CouponUsageRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CouponRedemptionService {

    private static final String REDEEMED_STATUS = "REDEEMED";

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponCountryRestrictionService countryRestrictionService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public CouponRedemptionService(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            CouponCountryRestrictionService countryRestrictionService,
            Clock clock,
            TransactionTemplate transactionTemplate
    ) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.countryRestrictionService = countryRestrictionService;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public RedeemCouponResponse redeemCoupon(String rawCode, String userId, String ipAddress) {
        validateUserId(userId);
        String code = normalizeForLookup(rawCode);
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, HttpStatus.NOT_FOUND));
        String resolvedCountryCode = countryRestrictionService.verifyCouponCountry(coupon, ipAddress);

        return transactionTemplate.execute(status -> redeemWithLock(code, userId, ipAddress, resolvedCountryCode));
    }

    private RedeemCouponResponse redeemWithLock(
            String code,
            String userId,
            String ipAddress,
            String resolvedCountryCode
    ) {
        Coupon coupon = couponRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (coupon.hasReachedUsageLimit()) {
            throw new BusinessException(ErrorCode.COUPON_USAGE_LIMIT_REACHED, HttpStatus.FORBIDDEN);
        }

        Instant redeemedAt = clock.instant();
        CouponUsage usage = new CouponUsage(coupon, userId, redeemedAt, ipAddress, resolvedCountryCode);
        try {
            couponUsageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCouponUsage(exception)) {
                throw new BusinessException(ErrorCode.COUPON_ALREADY_REDEEMED, HttpStatus.FORBIDDEN, exception);
            }
            throw exception;
        }

        coupon.incrementCurrentUses();
        return new RedeemCouponResponse(coupon.code(), userId, redeemedAt, REDEEMED_STATUS);
    }

    private String normalizeForLookup(String rawCode) {
        if (rawCode == null || rawCode.trim().isBlank()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isEmpty() || userId.length() > 100) {
            throw new IllegalArgumentException("User ID length must be 1-100 characters.");
        }
    }

    private boolean isDuplicateCouponUsage(DataIntegrityViolationException exception) {
        return exception.getCause() instanceof ConstraintViolationException constraintViolationException
                && hasConstraintName(constraintViolationException, "uk_coupon_usages_coupon_user");
    }

    private boolean hasConstraintName(ConstraintViolationException exception, String constraintName) {
        String actualConstraintName = exception.getConstraintName();
        return actualConstraintName != null
                && actualConstraintName.toLowerCase(Locale.ROOT).contains(constraintName);
    }
}
