package org.example.discountcode.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.api.response.CouponResponse;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-14T12:00:00Z");

    @Mock
    private CouponRepository couponRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, clock);
    }

    @Test
    void createsCouponWithNormalizedCodeAndCountry() {
        when(couponRepository.saveAndFlush(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CouponResponse response = couponService.createCoupon(new CreateCouponRequest("save10", 5, "pl"));

        assertThat(response).isEqualTo(new CouponResponse("SAVE10", NOW, 5, 0, "PL"));

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).saveAndFlush(couponCaptor.capture());
        assertThat(couponCaptor.getValue().code()).isEqualTo("SAVE10");
        assertThat(couponCaptor.getValue().countryCode()).isEqualTo("PL");
    }

    @Test
    void mapsDuplicateCouponInsertToConflict() {
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "Duplicate coupon code.",
                new SQLException("Duplicate entry.", "23000", 1062),
                "uk_coupons_code"
        );
        when(couponRepository.saveAndFlush(any(Coupon.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate coupon code.", constraintViolationException));

        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("save10", 5, "PL")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATE_COUPON_CODE);
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void rejectsSpacesInCreatedCouponCode() {
        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest(" SAVE10 ", 5, "PL")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidMaxUsesAtServiceBoundary() {
        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("SAVE10", 0, "PL")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(couponRepository, never()).saveAndFlush(any());
    }

    @Test
    void mapsConcurrentDuplicateCouponInsertToConflict() {
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "Duplicate coupon code.",
                new SQLException("Duplicate entry.", "23000", 1062),
                "uk_coupons_code"
        );
        when(couponRepository.saveAndFlush(any(Coupon.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate coupon code.", constraintViolationException));

        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("SAVE10", 5, "PL")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATE_COUPON_CODE);
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void doesNotMapUnrelatedIntegrityViolationToDuplicateCouponCode() {
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "Max uses constraint failed.",
                new SQLException("Check constraint failed.", "23000", 4025),
                "chk_coupons_max_uses_positive"
        );
        when(couponRepository.saveAndFlush(any(Coupon.class)))
                .thenThrow(new DataIntegrityViolationException("Check constraint failed.", constraintViolationException));

        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("SAVE10", 5, "PL")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fetchesCouponWithNormalizedCode() {
        Coupon coupon = new Coupon("SAVE10", 5, "PL", NOW);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.getCoupon("save10");

        assertThat(response.code()).isEqualTo("SAVE10");
    }

    @Test
    void lookupWithSpacesReturnsNotFound() {
        assertThatThrownBy(() -> couponService.getCoupon(" save10 "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND);
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void blankLookupReturnsNotFound() {
        assertThatThrownBy(() -> couponService.getCoupon(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND);
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void rejectsInvalidCountryCode() {
        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("SAVE10", 5, "XX")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankCountryCode() {
        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponRequest("SAVE10", 5, " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
