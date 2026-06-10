-- Flash deals (Phase 8c): a time-boxed sale price on a product. Used as the effective
-- price only inside [sale_starts_at, sale_ends_at]; outside the window the list price
-- applies. All nullable — a product has no deal until one is scheduled.
ALTER TABLE products ADD COLUMN sale_price     DECIMAL(10, 2);
ALTER TABLE products ADD COLUMN sale_starts_at TIMESTAMPTZ;
ALTER TABLE products ADD COLUMN sale_ends_at   TIMESTAMPTZ;
