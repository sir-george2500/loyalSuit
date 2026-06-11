-- HRM leave (Phase 9c). Tenant-defined leave types with an annual day allowance, and
-- per-employee leave requests drawn against them. Balances are computed (allowance minus
-- approved days taken this year), so there's no separate balance table to keep in sync.
CREATE TABLE leave_types (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                  VARCHAR(100) NOT NULL,
    annual_allowance_days INTEGER NOT NULL,
    paid                  BOOLEAN NOT NULL DEFAULT TRUE,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leave_types_tenant ON leave_types(tenant_id, name);

CREATE TABLE leave_requests (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id   UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    days          INTEGER NOT NULL,
    reason        VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_note VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- The admin queue (by status) and per-employee balance/history scans.
CREATE INDEX idx_leave_requests_tenant_status ON leave_requests(tenant_id, status, created_at DESC);
CREATE INDEX idx_leave_requests_employee ON leave_requests(tenant_id, employee_id, start_date);
