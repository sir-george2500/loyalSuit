-- Snapshot the selling vendor onto each order line at purchase time, so commission
-- settlement is immune to a later product re-assignment. NULL = house (non-vendor)
-- product. This mirrors products.vendor_id (the vendor's user id, no FK to vendors).
ALTER TABLE order_items ADD COLUMN vendor_id UUID;
CREATE INDEX idx_order_items_vendor_id ON order_items(vendor_id);

-- Commission ledger: one auditable entry per vendor order line. Earned when the
-- order's cash is collected (PAID); reversed if the order is later refunded. The
-- rate and amounts are snapshotted so the math stays auditable even if the vendor's
-- rate changes afterwards. order_item_id is unique, making settlement idempotent.
CREATE TABLE commission_entries (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id         UUID NOT NULL,
    order_id          UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    order_item_id     UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    order_number      VARCHAR(50) NOT NULL,
    gross_amount      DECIMAL(10, 2) NOT NULL,
    commission_rate   DECIMAL(5, 2) NOT NULL,
    commission_amount DECIMAL(10, 2) NOT NULL,
    net_amount        DECIMAL(10, 2) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'EARNED',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_commission_order_item UNIQUE (order_item_id)
);

CREATE INDEX idx_commission_tenant ON commission_entries(tenant_id);
CREATE INDEX idx_commission_vendor ON commission_entries(tenant_id, vendor_id);
CREATE INDEX idx_commission_order ON commission_entries(order_id);
