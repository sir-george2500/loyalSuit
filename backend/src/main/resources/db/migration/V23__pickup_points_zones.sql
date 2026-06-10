-- Pickup points and their delivery zones (Phase 7e). A store runs one or more pickup
-- points; each point owns its own zones (area + fee), so two points can price the same
-- city differently. Checkout (7e) prices shipping from the chosen point's zone.
CREATE TABLE pickup_points (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(500),
    phone      VARCHAR(30),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pickup_points_tenant ON pickup_points(tenant_id, created_at DESC);

-- A zone belongs to a point (cascades when the point is removed) — each point's own areas.
CREATE TABLE delivery_zones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    pickup_point_id UUID NOT NULL REFERENCES pickup_points(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    fee             DECIMAL(10, 2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_zones_point ON delivery_zones(pickup_point_id);
CREATE INDEX idx_delivery_zones_tenant ON delivery_zones(tenant_id);
