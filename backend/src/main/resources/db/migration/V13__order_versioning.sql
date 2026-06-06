-- Optimistic-locking version on orders so concurrent status changes can't both
-- apply (e.g. two simultaneous cancellations double-releasing reserved stock).
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- The admin order list is sorted newest-first within a tenant; back it with an index.
CREATE INDEX idx_orders_tenant_created ON orders(tenant_id, created_at DESC);
