package org.example.discountcode.coupon.infrastructure;

import org.example.discountcode.coupon.domain.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
}
