-- BUUR-96: Backoffice Dashboard v2 — per-user layout + action-item snooze
-- Additive only; safe to roll back.
-- backoffice_user_id is the Keycloak subject UUID of the acting Buurmy. Backoffice
-- admins are not necessarily rows in the tenant `users` table, so there is no FK
-- (matches impersonation_sessions.admin_user_id).

CREATE TABLE backoffice_user_dashboard_layout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backoffice_user_id UUID NOT NULL,
    layout JSONB NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_backoffice_dashboard_layout_user UNIQUE (backoffice_user_id)
);

CREATE TABLE backoffice_action_item_snooze (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backoffice_user_id UUID NOT NULL,
    item_key VARCHAR(120) NOT NULL,
    snoozed_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_backoffice_action_snooze UNIQUE (backoffice_user_id, item_key)
);

CREATE INDEX idx_action_snooze_user ON backoffice_action_item_snooze (backoffice_user_id, snoozed_until);
