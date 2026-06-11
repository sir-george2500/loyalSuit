-- HRM awards (Phase 9e). Recognition recorded against an employee and shown on their profile.
-- Simple standalone records with no lifecycle.
CREATE TABLE awards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL,
    category    VARCHAR(20) NOT NULL,
    awarded_on  DATE NOT NULL,
    description VARCHAR(1000),
    awarded_by  VARCHAR(150),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- An employee's awards, most recent first.
CREATE INDEX idx_awards_employee ON awards(tenant_id, employee_id, awarded_on DESC);
