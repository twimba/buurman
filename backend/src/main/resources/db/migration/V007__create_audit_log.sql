CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_fields JSONB,
    old_values JSONB,
    new_values JSONB,
    user_id UUID NOT NULL REFERENCES users (id),
    TIMESTAMP TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_audit_log_action CHECK (
        action IN ('CREATE', 'UPDATE', 'DELETE', 'RESTORE')
    )
);

CREATE INDEX idx_audit_log_team ON audit_log (team_id);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);

CREATE INDEX idx_audit_log_timestamp ON audit_log (TIMESTAMP DESC);
