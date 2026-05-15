package org.example.discountcode.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.example.discountcode.common.BusinessException;
import org.example.discountcode.common.ErrorCode;
import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.domain.Coupon;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.example.discountcode.coupon.infrastructure.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.geoip.provider=stub",
        "app.geoip.stub.default-country-code=PL"
})
class CouponRedemptionConcurrencyIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRedemptionService redemptionService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    @BeforeEach
    void cleanDatabase() {
        couponUsageRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    void concurrentRedemptionsCannotExceedMaxUses() throws Exception {
        couponService.createCoupon(new CreateCouponRequest("HOTDEAL", 3, "PL"));

        List<Result> results = runConcurrently(12, index ->
                redemptionService.redeemCoupon("HOTDEAL", "user-" + index, "203.0.113.10"));

        assertThat(results.stream().filter(Result::isSuccess)).hasSize(3);
        assertThat(results.stream().filter(result -> result.errorCode() == ErrorCode.COUPON_USAGE_LIMIT_REACHED))
                .hasSize(9);
        assertThat(couponUsageRepository.count()).isEqualTo(3);
        Coupon coupon = couponRepository.findByCode("HOTDEAL").orElseThrow();
        assertThat(coupon.currentUses()).isEqualTo(3);
    }

    @Test
    void duplicateUserRedemptionCreatesOnlyOneUsageRow() throws Exception {
        couponService.createCoupon(new CreateCouponRequest("ONCEONLY", 10, "PL"));

        List<Result> results = runConcurrently(8, index ->
                redemptionService.redeemCoupon("ONCEONLY", "same-user", "203.0.113.10"));

        assertThat(results.stream().filter(Result::isSuccess)).hasSize(1);
        assertThat(results.stream().filter(result -> result.errorCode() == ErrorCode.COUPON_ALREADY_REDEEMED))
                .hasSize(7);
        assertThat(couponUsageRepository.count()).isEqualTo(1);
        Coupon coupon = couponRepository.findByCode("ONCEONLY").orElseThrow();
        assertThat(coupon.currentUses()).isEqualTo(1);
    }

    private List<Result> runConcurrently(int workers, ThrowingIntConsumer task) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Result>> futures = java.util.stream.IntStream.range(0, workers)
                    .mapToObj(index -> (Callable<Result>) () -> {
                        ready.countDown();
                        start.await();
                        try {
                            task.accept(index);
                            return Result.succeeded();
                        } catch (BusinessException exception) {
                            return Result.failure(exception.errorCode());
                        }
                    })
                    .map(executorService::submit)
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
        } finally {
            executorService.shutdownNow();
        }
    }

    private record Result(boolean success, ErrorCode errorCode) {

        boolean isSuccess() {
            return success;
        }

        static Result succeeded() {
            return new Result(true, null);
        }

        static Result failure(ErrorCode errorCode) {
            return new Result(false, errorCode);
        }
    }

    @FunctionalInterface
    private interface ThrowingIntConsumer {

        void accept(int value);
    }
}
