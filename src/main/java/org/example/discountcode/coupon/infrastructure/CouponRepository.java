package org.example.discountcode.coupon.infrastructure;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.example.discountcode.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coupon from Coupon coupon where coupon.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}
