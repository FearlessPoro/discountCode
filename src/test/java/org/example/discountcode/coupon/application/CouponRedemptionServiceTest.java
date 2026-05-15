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
import org.example.discountcode.coupon.api.response.RedeemCouponResponse;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.coupon.domain.CouponUsage;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.example.discountcode.coupon.infrastructure.CouponUsageRepository;
import org.example.discountcode.testdata.DemoTestData;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-14T12:00:00Z");

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CouponCountryRestrictionService countryRestrictionService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private CouponRedemptionService service;

    @BeforeEach
    void setUp() {
        service = new CouponRedemptionService(
                couponRepository,
                couponUsageRepository,
                countryRestrictionService,
                clock,
                new TransactionTemplate(new ImmediateTransactionManager())
        );
    }

    @Test
    void redeemsCouponWithNormalizedTrimmedCode() {
        Coupon coupon = DemoTestData.plTestCoupon(NOW);
        when(couponRepository.findByCode(DemoTestData.PLTEST_CODE)).thenReturn(Optional.of(coupon));
        when(countryRestrictionService.verifyCouponCountry(coupon, "203.0.113.10"))
                .thenReturn(DemoTestData.PL_COUNTRY);
        when(couponRepository.findByCodeForUpdate(DemoTestData.PLTEST_CODE)).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RedeemCouponResponse response = service.redeemCoupon(" pltest ", DemoTestData.USER_1, "203.0.113.10");

        assertThat(response).isEqualTo(new RedeemCouponResponse(
                DemoTestData.PLTEST_CODE,
                DemoTestData.USER_1,
                NOW,
                "REDEEMED"
        ));
        assertThat(coupon.currentUses()).isEqualTo(1);

        ArgumentCaptor<CouponUsage> usageCaptor = ArgumentCaptor.forClass(CouponUsage.class);
        verify(couponUsageRepository).saveAndFlush(usageCaptor.capture());
        assertThat(usageCaptor.getValue().coupon()).isSameAs(coupon);
        assertThat(usageCaptor.getValue().userId()).isEqualTo(DemoTestData.USER_1);
        assertThat(usageCaptor.getValue().usedAt()).isEqualTo(NOW);
        assertThat(usageCaptor.getValue().ipAddress()).isEqualTo("203.0.113.10");
        assertThat(usageCaptor.getValue().resolvedCountryCode()).isEqualTo(DemoTestData.PL_COUNTRY);
    }

    @Test
    void blankRedeemCodeReturnsNotFound() {
        assertThatThrownBy(() -> service.redeemCoupon(" ", "user-1", "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND);
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(couponRepository, never()).findByCode(any());
    }

    @Test
    void missingCouponReturnsNotFoundBeforeCountryLookup() {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemCoupon("SAVE10", "user-1", "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND);
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(countryRestrictionService, never()).verifyCouponCountry(any(), any());
    }

    @Test
    void rejectsInvalidUserIdAtServiceBoundary() {
        assertThatThrownBy(() -> service.redeemCoupon("SAVE10", "", "203.0.113.10"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(couponRepository, never()).findByCode(any());
    }

    @Test
    void reachedUsageLimitReturnsForbidden() {
        Coupon coupon = DemoTestData.limitOneCoupon(NOW);
        coupon.incrementCurrentUses();
        when(couponRepository.findByCode(DemoTestData.LIMIT1_CODE)).thenReturn(Optional.of(coupon));
        when(countryRestrictionService.verifyCouponCountry(coupon, "203.0.113.10"))
                .thenReturn(DemoTestData.US_COUNTRY);
        when(couponRepository.findByCodeForUpdate(DemoTestData.LIMIT1_CODE)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.redeemCoupon(DemoTestData.LIMIT1_CODE, DemoTestData.USER_1, "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_USAGE_LIMIT_REACHED);
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(couponUsageRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateUsageReturnsForbiddenAndDoesNotIncrementUsageCount() {
        Coupon coupon = DemoTestData.plTestCoupon(NOW);
        when(couponRepository.findByCode(DemoTestData.PLTEST_CODE)).thenReturn(Optional.of(coupon));
        when(countryRestrictionService.verifyCouponCountry(coupon, "203.0.113.10"))
                .thenReturn(DemoTestData.PL_COUNTRY);
        when(couponRepository.findByCodeForUpdate(DemoTestData.PLTEST_CODE)).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenThrow(duplicateUsageViolation());

        assertThatThrownBy(() -> service.redeemCoupon(DemoTestData.PLTEST_CODE, DemoTestData.USER_1, "203.0.113.10"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.COUPON_ALREADY_REDEEMED);
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        assertThat(coupon.currentUses()).isZero();
    }

    private DataIntegrityViolationException duplicateUsageViolation() {
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "Duplicate coupon usage.",
                new SQLException("Duplicate entry.", "23000", 1062),
                "uk_coupon_usages_coupon_user"
        );
        return new DataIntegrityViolationException("Duplicate coupon usage.", constraintViolationException);
    }

    private static class ImmediateTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
