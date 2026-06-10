-- Loyalty points (Phase 8d). A registered customer earns points when their orders are
-- paid and spends them on discounts; the account holds the running balance and the
-- transactions table is the append-only ledger behind it.
CREATE TABLE loyalty_accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL,
    points      INTEGER NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One account per customer in a store.
CREATE UNIQUE INDEX idx_loyalty_accounts_customer ON loyalty_accounts(customer_id);

CREATE TABLE loyalty_transactions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES loyalty_accounts(id) ON DELETE CASCADE,
    order_id   UUID,
    type       VARCHAR(20) NOT NULL,
    points     INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_tx_account ON loyalty_transactions(tenant_id, account_id, created_at DESC);
-- Accrual / reversal are idempotent per order: one EARN and one REVERSE per order.
CREATE INDEX idx_loyalty_tx_order ON loyalty_transactions(order_id, type);
