-- HRM attendance (Phase 9b). One row per employee per work day: check-in / check-out are
-- stamped on clock actions or entered by an admin. Worked hours are derived, not stored.
CREATE TABLE attendance (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    work_date   DATE NOT NULL,
    check_in    TIMESTAMPTZ,
    check_out   TIMESTAMPTZ,
    status      VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- At most one attendance record per employee per day.
    CONSTRAINT uq_attendance_employee_day UNIQUE (tenant_id, employee_id, work_date)
);

-- Timesheet queries scan an employee's days within a range.
CREATE INDEX idx_attendance_employee_date ON attendance(tenant_id, employee_id, work_date);
