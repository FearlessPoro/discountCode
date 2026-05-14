package org.example.discountcode.coupon.infrastructure;

import java.util.Optional;
import org.example.discountcode.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);
}
