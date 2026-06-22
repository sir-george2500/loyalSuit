-- Per-tenant notification preferences (Phase 13). One row per tenant; created lazily with
-- sensible defaults the first time the settings screen is opened or saved.
CREATE TABLE notification_preferences (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    order_confirmation_email BOOLEAN NOT NULL DEFAULT TRUE,
    order_status_email       BOOLEAN NOT NULL DEFAULT TRUE,
    low_stock_alert          BOOLEAN NOT NULL DEFAULT TRUE,
    new_review_alert         BOOLEAN NOT NULL DEFAULT TRUE,
    payout_alert             BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_email          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
