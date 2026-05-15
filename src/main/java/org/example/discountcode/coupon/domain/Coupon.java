package org.example.discountcode.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    protected Coupon() {
    }

    public Coupon(String code, int maxUses, String countryCode, Instant createdAt) {
        this.code = code;
        this.maxUses = maxUses;
        this.countryCode = countryCode;
        this.createdAt = createdAt;
        this.currentUses = 0;
    }

    public Long id() {
        return id;
    }

    public String code() {
        return code;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int maxUses() {
        return maxUses;
    }

    public int currentUses() {
        return currentUses;
    }

    public String countryCode() {
        return countryCode;
    }

    public boolean hasReachedUsageLimit() {
        return currentUses >= maxUses;
    }

    public void incrementCurrentUses() {
        if (hasReachedUsageLimit()) {
            throw new IllegalStateException("Coupon usage limit has been reached.");
        }
        currentUses++;
    }
}
