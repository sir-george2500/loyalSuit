-- Cash-drawer sessions. A cashier opens a shift with a starting float, rings up cash
-- sales against it, then closes it by counting the drawer. Closing reconciles the till:
--   expected_cash = opening_float + sales_total;  variance = counted_cash - expected_cash.
-- version guards the close so a drawer can't be reconciled twice concurrently.
CREATE TABLE pos_shifts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    cashier_id    UUID NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opening_float DECIMAL(10, 2) NOT NULL,
    closed_at     TIMESTAMPTZ,
    sales_total   DECIMAL(10, 2),
    sale_count    INTEGER NOT NULL DEFAULT 0,
    expected_cash DECIMAL(10, 2),
    counted_cash  DECIMAL(10, 2),
    variance      DECIMAL(10, 2),
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pos_shifts_tenant ON pos_shifts(tenant_id, created_at DESC);

-- At most one OPEN drawer per cashier at a time — the backstop against two concurrent
-- opens, and what makes "the cashier's open shift" an unambiguous lookup.
CREATE UNIQUE INDEX idx_pos_shifts_one_open ON pos_shifts(tenant_id, cashier_id)
    WHERE status = 'OPEN';

-- Link each sale to its drawer. Nullable so the column can be added safely to a table
-- that may already hold rows; every new sale sets it (enforced in the domain).
ALTER TABLE pos_sales ADD COLUMN shift_id UUID;
CREATE INDEX idx_pos_sales_shift ON pos_sales(tenant_id, shift_id);
