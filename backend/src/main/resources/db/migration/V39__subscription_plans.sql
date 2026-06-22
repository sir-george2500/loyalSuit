-- Platform subscription plans (Phase 13). Global catalogue managed by the platform owner;
-- distinct from the legacy per-tenant SubscriptionPlan enum on the tenants table.
CREATE TABLE subscription_plans (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50) NOT NULL UNIQUE,
    name             VARCHAR(120) NOT NULL,
    description      TEXT,
    price            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    currency         VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    max_products     INTEGER,
    max_staff        INTEGER,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
