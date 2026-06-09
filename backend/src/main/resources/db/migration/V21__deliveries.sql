-- Deliveries (Phase 7b). One delivery per order — the courier-side record layered on the
-- orders ledger. It carries the assigned agent and walks a guarded status lifecycle
-- (PENDING -> ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED / FAILED); version guards
-- concurrent transitions. Order number, customer name, and agent name are snapshotted for
-- display without cross-module reads.
CREATE TABLE deliveries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id       UUID NOT NULL,
    order_number   VARCHAR(100) NOT NULL,
    customer_name  VARCHAR(255),
    agent_id       UUID,
    agent_name     VARCHAR(255),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_at    TIMESTAMPTZ,
    picked_up_at   TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    failed_at      TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One delivery per order.
CREATE UNIQUE INDEX idx_deliveries_order ON deliveries(order_id);
CREATE INDEX idx_deliveries_tenant ON deliveries(tenant_id, created_at DESC);
CREATE INDEX idx_deliveries_agent ON deliveries(tenant_id, agent_id);
CREATE INDEX idx_deliveries_status ON deliveries(tenant_id, status);

-- Append-only status timeline: the audit trail of how a delivery progressed.
CREATE TABLE delivery_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    delivery_id UUID NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL,
    note        VARCHAR(500),
    actor_id    UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_events_delivery ON delivery_events(delivery_id, created_at);
