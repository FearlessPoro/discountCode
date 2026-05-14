CREATE TABLE coupon_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    used_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ip_address VARCHAR(45) NOT NULL,
    resolved_country_code VARCHAR(2) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_coupon_usages_coupon_id FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT uk_coupon_usages_coupon_user UNIQUE (coupon_id, user_id),
    CONSTRAINT chk_coupon_usages_user_id_length CHECK (CHAR_LENGTH(user_id) BETWEEN 1 AND 100),
    CONSTRAINT chk_coupon_usages_resolved_country_code_format CHECK (resolved_country_code REGEXP '^[A-Z]{2}$')
);
