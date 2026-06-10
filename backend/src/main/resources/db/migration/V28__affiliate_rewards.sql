-- Affiliate attribution + reward ledger (Phase 8e.2). An order can be attributed to an
-- affiliate (set from a referral code at checkout); when it's paid the affiliate earns a
-- reward, reversed if the order is refunded. Mirrors the commission ledger.
ALTER TABLE orders ADD COLUMN affiliate_id UUID;

CREATE TABLE affiliate_rewards (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    affiliate_id  UUID NOT NULL REFERENCES affiliates(id) ON DELETE CASCADE,
    order_id      UUID NOT NULL,
    order_number  VARCHAR(100) NOT NULL,
    gross_amount  DECIMAL(10, 2) NOT NULL,
    reward_rate   DECIMAL(5, 2) NOT NULL,
    reward_amount DECIMAL(10, 2) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'EARNED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_affiliate_rewards_affiliate ON affiliate_rewards(tenant_id, affiliate_id, created_at DESC);
-- One earning per order (accrual is idempotent).
CREATE UNIQUE INDEX idx_affiliate_rewards_order ON affiliate_rewards(order_id);
