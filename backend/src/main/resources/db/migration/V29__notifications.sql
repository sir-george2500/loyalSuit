-- In-app notifications (Phase 8f). One row per message to a user; the inbox reads the
-- recipient's rows newest-first and tracks a read flag.
CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    type         VARCHAR(40) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         VARCHAR(1000),
    link         VARCHAR(255),
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(tenant_id, recipient_id, created_at DESC);
-- Fast unread badge.
CREATE INDEX idx_notifications_unread ON notifications(tenant_id, recipient_id) WHERE is_read = FALSE;
