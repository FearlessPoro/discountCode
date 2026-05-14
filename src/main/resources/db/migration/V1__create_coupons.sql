CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    max_uses INT NOT NULL,
    current_uses INT NOT NULL DEFAULT 0,
    country_code VARCHAR(2) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT chk_coupons_code_length CHECK (CHAR_LENGTH(code) BETWEEN 1 AND 50),
    CONSTRAINT chk_coupons_code_format CHECK (code REGEXP '^[A-Z0-9]+$'),
    CONSTRAINT chk_coupons_max_uses_positive CHECK (max_uses > 0),
    CONSTRAINT chk_coupons_current_uses_non_negative CHECK (current_uses >= 0),
    CONSTRAINT chk_coupons_current_uses_not_over_max CHECK (current_uses <= max_uses),
    CONSTRAINT chk_coupons_country_code_format CHECK (country_code REGEXP '^[A-Z]{2}$')
);
