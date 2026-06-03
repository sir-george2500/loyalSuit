CREATE TABLE vendors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    store_name      VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    logo_url        VARCHAR(500),
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vendors_tenant_id ON vendors(tenant_id);
CREATE INDEX idx_vendors_user_id ON vendors(user_id);

CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_number     VARCHAR(50) NOT NULL UNIQUE,
    customer_id      UUID NOT NULL REFERENCES app_users(id),
    vendor_id        UUID REFERENCES vendors(id),
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    subtotal         DECIMAL(10, 2) NOT NULL,
    shipping_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax_amount       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total            DECIMAL(10, 2) NOT NULL,
    currency         CHAR(3) NOT NULL DEFAULT 'USD',
    shipping_address JSONB,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    variant_id UUID REFERENCES product_variants(id),
    quantity   INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total      DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
