-- HRM employees (Phase 9). A person who works for the store, optionally linked to a login
-- user. base_salary is the monthly figure later HRM slices (leave, payroll) draw on.
CREATE TABLE employees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(40),
    job_title       VARCHAR(255) NOT NULL,
    department      VARCHAR(255),
    employment_type VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    hire_date       DATE NOT NULL,
    base_salary     DECIMAL(12, 2) NOT NULL,
    user_id         UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_tenant ON employees(tenant_id, created_at DESC);
CREATE INDEX idx_employees_status ON employees(tenant_id, status);
