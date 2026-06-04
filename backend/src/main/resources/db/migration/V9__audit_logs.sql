-- Append-only security/business audit trail. tenant_id and actor_id are nullable
-- because some events are pre-auth (e.g. a failed login for an unknown account).
-- actor_id is intentionally NOT a foreign key: audit records must survive the
-- deletion of the user they reference.
CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id      UUID,
    actor_email   VARCHAR(255),
    actor_role    VARCHAR(50),
    action        VARCHAR(64) NOT NULL,
    outcome       VARCHAR(16) NOT NULL,
    resource_type VARCHAR(64),
    resource_id   VARCHAR(64),
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(512),
    detail        TEXT,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_occurred ON audit_logs(tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
