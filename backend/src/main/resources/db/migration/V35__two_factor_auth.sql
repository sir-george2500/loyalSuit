-- Two-factor auth (Phase 10b). The TOTP secret and enabled flag live on the user; recovery
-- codes are a separate table, stored only as hashes and consumed (used_at set) on use.
ALTER TABLE app_users ADD COLUMN two_factor_secret  VARCHAR(64);
ALTER TABLE app_users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE two_factor_recovery_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_2fa_recovery_user ON two_factor_recovery_codes(user_id);
