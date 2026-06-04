-- Tenant onboarding: company localization + a completion marker.
-- A NULL onboarded_at means the tenant still needs to run the setup wizard.
ALTER TABLE tenants
    ADD COLUMN currency     VARCHAR(3)  NOT NULL DEFAULT 'USD',
    ADD COLUMN country      VARCHAR(2),
    ADD COLUMN timezone     VARCHAR(64) NOT NULL DEFAULT 'UTC',
    ADD COLUMN phone        VARCHAR(40),
    ADD COLUMN onboarded_at TIMESTAMPTZ;
