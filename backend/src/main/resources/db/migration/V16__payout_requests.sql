-- Vendor payout requests drawn against settled commission. A vendor requests an
-- amount up to their available balance (earned net, less pending/paid payouts); an
-- admin pays it (cash) or rejects it. decided_by / decided_at + the audit log form
-- the trail. version guards the decision against concurrent double-payment.
CREATE TABLE payout_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id       UUID NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference       VARCHAR(255),
    resolution_note TEXT,
    decided_by      UUID,
    decided_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payouts_tenant ON payout_requests(tenant_id);
CREATE INDEX idx_payouts_vendor ON payout_requests(tenant_id, vendor_id);
CREATE INDEX idx_payouts_status ON payout_requests(tenant_id, status);

-- At most one PENDING request per vendor at a time. This is the hard backstop against
-- two concurrent requests both passing the available-balance check and over-drawing —
-- the second insert fails atomically rather than over-committing funds.
CREATE UNIQUE INDEX idx_payouts_one_pending ON payout_requests(tenant_id, vendor_id)
    WHERE status = 'PENDING';
