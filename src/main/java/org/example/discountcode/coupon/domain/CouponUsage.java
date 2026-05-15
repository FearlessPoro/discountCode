package org.example.discountcode.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_usages_coupon_user",
                columnNames = {"coupon_id", "user_id"}
        )
)
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "resolved_country_code", nullable = false, length = 2)
    private String resolvedCountryCode;

    protected CouponUsage() {
    }

    public CouponUsage(Coupon coupon, String userId, Instant usedAt, String ipAddress, String resolvedCountryCode) {
        this.coupon = coupon;
        this.userId = userId;
        this.usedAt = usedAt;
        this.ipAddress = ipAddress;
        this.resolvedCountryCode = resolvedCountryCode;
    }

    public Long id() {
        return id;
    }

    public Coupon coupon() {
        return coupon;
    }

    public String userId() {
        return userId;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public String ipAddress() {
        return ipAddress;
    }

    public String resolvedCountryCode() {
        return resolvedCountryCode;
    }
}
