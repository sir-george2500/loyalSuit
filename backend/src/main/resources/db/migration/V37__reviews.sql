-- Product reviews (Phase 13e). A customer rates a product they received; one review per
-- customer per product. vendor_id is the selling Vendor PK snapshotted from the purchase
-- (null for a house product) so a seller can read reviews of their products without joining
-- back to orders. author_name is a privacy-minimised display name captured at write time.
CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL,
    customer_id UUID NOT NULL,
    vendor_id   UUID,
    author_name VARCHAR(255) NOT NULL,
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_customer_product UNIQUE (tenant_id, customer_id, product_id)
);

-- Public product page reads newest-first by product.
CREATE INDEX idx_reviews_product ON reviews(product_id, created_at DESC);
-- Seller "Reviews" reads newest-first by vendor within the tenant.
CREATE INDEX idx_reviews_vendor ON reviews(tenant_id, vendor_id, created_at DESC);
