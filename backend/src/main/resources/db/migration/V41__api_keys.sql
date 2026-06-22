-- Tenant API keys (Phase 13). Only a SHA-256 hash of the key is stored; the plaintext is
-- shown to the caller once at creation and never again. key_prefix + last_four allow the UI
-- to identify a key without revealing it. revoked_at set => the key no longer authenticates.
CREATE TABLE api_keys (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    key_hash     VARCHAR(100) NOT NULL UNIQUE,
    key_prefix   VARCHAR(16) NOT NULL,
    last_four    VARCHAR(4) NOT NULL,
    created_by   UUID,
    last_used_at TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id, created_at DESC);
