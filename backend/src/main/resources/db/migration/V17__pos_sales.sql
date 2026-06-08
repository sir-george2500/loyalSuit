-- In-store (POS) cash sales. The money, line items, and commission live on the linked
-- orders row (the single sales ledger); a pos_sales row is the channel record: who rang
-- it up, how much cash was taken, and the change given. It is also the source for cash
-- drawer reconciliation (a later slice).
CREATE TABLE pos_sales (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders(id),
    order_number    VARCHAR(255) NOT NULL,
    cashier_id      UUID NOT NULL,
    client_sale_id  VARCHAR(100) NOT NULL,
    subtotal        DECIMAL(10, 2) NOT NULL,
    total           DECIMAL(10, 2) NOT NULL,
    amount_tendered DECIMAL(10, 2) NOT NULL,
    change_given    DECIMAL(10, 2) NOT NULL,
    item_count      INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- The terminal-supplied client_sale_id makes completing a sale idempotent. This unique
-- index is the hard backstop: a re-submitted sale (e.g. an offline replay) can't ring up
-- twice — the second insert fails atomically rather than double-charging or double-stocking.
CREATE UNIQUE INDEX idx_pos_sales_client ON pos_sales(tenant_id, client_sale_id);
CREATE INDEX idx_pos_sales_tenant ON pos_sales(tenant_id, created_at DESC);
CREATE INDEX idx_pos_sales_order ON pos_sales(order_id);
