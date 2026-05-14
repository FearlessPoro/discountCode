package org.example.discountcode.coupon.application;

import java.time.Clock;
import java.util.Locale;
import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.api.response.CouponResponse;
import org.example.discountcode.coupon.domain.CountryCode;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.coupon.domain.CouponCode;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final Clock clock;

    public CouponService(CouponRepository couponRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String code = CouponCode.normalizeForCreation(request.code());
        String countryCode = CountryCode.normalize(request.countryCode());
        if (request.maxUses() == null || request.maxUses() <= 0) {
            throw new IllegalArgumentException("Max uses must be greater than 0.");
        }

        Coupon coupon = new Coupon(code, request.maxUses(), countryCode, clock.instant());
        try {
            return toResponse(couponRepository.saveAndFlush(coupon));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCouponCode(exception)) {
                throw new BusinessException(ErrorCode.DUPLICATE_COUPON_CODE, HttpStatus.CONFLICT, exception);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        String code = rawCode.toUpperCase(Locale.ROOT);
        return couponRepository.findByCode(code)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.code(),
                coupon.createdAt(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.countryCode()
        );
    }

    private boolean isDuplicateCouponCode(DataIntegrityViolationException exception) {
        return exception.getCause() instanceof ConstraintViolationException constraintViolationException
                && "uk_coupons_code".equals(constraintViolationException.getConstraintName());
    }
}
