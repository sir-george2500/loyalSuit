-- Product image gallery. Each row references a Cloudinary asset by its public_id
-- (used for deletion) and stores the secure delivery URL. tenant_id is carried
-- for direct tenant-scoping/defense-in-depth even though product_id implies it.
CREATE TABLE product_media (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    public_id  VARCHAR(255) NOT NULL,
    url        VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_media_product_id ON product_media(product_id);
CREATE INDEX idx_product_media_tenant_id ON product_media(tenant_id);
