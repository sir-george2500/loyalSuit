-- Coupons / discount codes (Phase 8a). A code is unique per tenant. A coupon may limit by
-- minimum subtotal, a discount cap (percent), a validity window, and usage limits. The
-- redemption ledger records each use to enforce those limits and audit the discount given.
CREATE TABLE coupons (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code               VARCHAR(64) NOT NULL,
    description        VARCHAR(255),
    discount_type      VARCHAR(20) NOT NULL,
    value              DECIMAL(10, 2) NOT NULL,
    min_order_subtotal DECIMAL(10, 2),
    max_discount       DECIMAL(10, 2),
    usage_limit        INTEGER,
    per_customer_limit INTEGER,
    starts_at          TIMESTAMPTZ,
    ends_at            TIMESTAMPTZ,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One coupon per code within a store.
CREATE UNIQUE INDEX idx_coupons_code ON coupons(tenant_id, code);
CREATE INDEX idx_coupons_tenant ON coupons(tenant_id, created_at DESC);

CREATE TABLE coupon_redemptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    coupon_id       UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    order_id        UUID NOT NULL,
    customer_email  VARCHAR(255),
    discount_amount DECIMAL(10, 2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One redemption per order (a retried checkout can't redeem twice).
CREATE UNIQUE INDEX idx_coupon_redemptions_order ON coupon_redemptions(order_id);
CREATE INDEX idx_coupon_redemptions_coupon ON coupon_redemptions(coupon_id);
CREATE INDEX idx_coupon_redemptions_customer ON coupon_redemptions(coupon_id, customer_email);
