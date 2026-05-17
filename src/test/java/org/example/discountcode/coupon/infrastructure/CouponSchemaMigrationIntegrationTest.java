package org.example.discountcode.coupon.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.discountcode.coupon.api.request.CreateCouponRequest;
import org.example.discountcode.coupon.application.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migration_validation;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.geoip.provider=stub",
        "app.geoip.stub.default-country-code=PL"
})
class CouponSchemaMigrationIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void flywaySchemaSupportsCouponPersistenceWithHibernateValidation() {
        couponService.createCoupon(new CreateCouponRequest("MIGRATION1", 2, "PL"));

        assertThat(couponRepository.findByCode("MIGRATION1"))
                .hasValueSatisfying(coupon -> {
                    assertThat(coupon.maxUses()).isEqualTo(2);
                    assertThat(coupon.currentUses()).isZero();
                    assertThat(coupon.countryCode()).isEqualTo("PL");
                });
    }
}
