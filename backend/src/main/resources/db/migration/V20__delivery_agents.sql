-- Delivery-agent roster (Phase 7a). Each row is a courier profile linked to a
-- DELIVERY_AGENT user (one profile per user); name/email live on that user. An inactive
-- agent stays on the roster with their history but isn't assignable.
CREATE TABLE delivery_agents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,
    phone        VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One agent profile per user.
CREATE UNIQUE INDEX idx_delivery_agents_user ON delivery_agents(user_id);
CREATE INDEX idx_delivery_agents_tenant ON delivery_agents(tenant_id, created_at DESC);
