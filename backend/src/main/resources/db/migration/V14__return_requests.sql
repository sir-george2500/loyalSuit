-- Return/refund requests. A guest requests a return against a delivered order
-- (verified by order number + email); an admin approves (refund + restock) or
-- rejects. Whole-order returns for now; per-item returns can extend this later.
CREATE TABLE return_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    order_number    VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason          TEXT NOT NULL,
    customer_email  VARCHAR(255),
    resolution_note TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_return_requests_tenant ON return_requests(tenant_id, created_at DESC);
CREATE INDEX idx_return_requests_order ON return_requests(order_id);
