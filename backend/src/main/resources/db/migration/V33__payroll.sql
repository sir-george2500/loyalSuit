-- HRM payroll (Phase 9d). A pay run over a period generates one payslip per active employee.
-- Run totals are denormalised so the runs list is a single query. All payslip figures are
-- snapshotted, so finalised pay is never rewritten by a later salary or leave change.
CREATE TABLE pay_runs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    label         VARCHAR(100) NOT NULL,
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    paid_at       TIMESTAMPTZ,
    payslip_count INTEGER NOT NULL DEFAULT 0,
    total_net     DECIMAL(14, 2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- One run per period; re-running the same month is blocked.
    CONSTRAINT uq_pay_runs_period UNIQUE (tenant_id, period_start, period_end)
);

CREATE INDEX idx_pay_runs_tenant ON pay_runs(tenant_id, period_start DESC);

CREATE TABLE payslips (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    pay_run_id             UUID NOT NULL REFERENCES pay_runs(id) ON DELETE CASCADE,
    employee_id            UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    employee_name          VARCHAR(255) NOT NULL,
    base_salary            DECIMAL(12, 2) NOT NULL,
    unpaid_leave_days      INTEGER NOT NULL DEFAULT 0,
    unpaid_leave_deduction DECIMAL(12, 2) NOT NULL DEFAULT 0,
    other_deductions       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    net_pay                DECIMAL(12, 2) NOT NULL DEFAULT 0,
    note                   VARCHAR(500),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payslips_run ON payslips(pay_run_id);
