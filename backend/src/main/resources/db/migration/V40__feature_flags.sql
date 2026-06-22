-- Platform feature flags (Phase 13). Global on/off switches managed by the platform owner.
CREATE TABLE feature_flags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key    VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
