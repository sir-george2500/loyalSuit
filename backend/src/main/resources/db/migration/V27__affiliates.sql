-- Affiliates (Phase 8e). A user who refers shoppers with a unique code and earns a reward
-- (a percentage of paid referred orders). One profile per user; the code is unique per tenant.
CREATE TABLE affiliates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    code        VARCHAR(64) NOT NULL,
    reward_rate DECIMAL(5, 2) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_affiliates_user ON affiliates(user_id);
CREATE UNIQUE INDEX idx_affiliates_code ON affiliates(tenant_id, code);
CREATE INDEX idx_affiliates_tenant ON affiliates(tenant_id, created_at DESC);
