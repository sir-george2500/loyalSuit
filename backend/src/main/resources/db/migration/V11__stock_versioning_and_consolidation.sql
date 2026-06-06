-- Optimistic-locking version for the stock rows. Used by JPA on absolute "set
-- level" writes and incremented by the atomic delta-adjust UPDATE.
ALTER TABLE stock ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Consolidate the dual stock model. Per-warehouse rows in the `stock` table are
-- the single source of truth; the denormalized per-variant column was never read
-- or written by any code, so it is removed.
ALTER TABLE product_variants DROP COLUMN stock_quantity;
