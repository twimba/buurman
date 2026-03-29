-- BUUR-20: User impersonation from backoffice
CREATE TABLE impersonation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL UNIQUE,
    admin_user_id UUID NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    admin_name VARCHAR(255) NOT NULL,
    target_user_id UUID NOT NULL REFERENCES users (id),
    target_team_id UUID NOT NULL REFERENCES teams (id),
    session_token UUID NOT NULL UNIQUE,
    mode VARCHAR(20) NOT NULL DEFAULT 'FULL',
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    activated_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    end_reason VARCHAR(20),
    CONSTRAINT chk_impersonation_mode CHECK (mode IN ('FULL', 'READ_ONLY')),
    CONSTRAINT chk_impersonation_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'ENDED', 'EXPIRED')
    ),
    CONSTRAINT chk_impersonation_end_reason CHECK (
        end_reason IS NULL
        OR end_reason IN ('MANUAL', 'EXPIRED', 'ADMIN_TERMINATED')
    )
);

CREATE INDEX idx_impersonation_sessions_admin ON impersonation_sessions (admin_user_id);

CREATE INDEX idx_impersonation_sessions_target ON impersonation_sessions (target_user_id);

CREATE INDEX idx_impersonation_sessions_token ON impersonation_sessions (session_token)
WHERE
    status = 'PENDING';

CREATE INDEX idx_impersonation_sessions_status ON impersonation_sessions (status)
WHERE
    status = 'ACTIVE';

-- Audit log: track impersonation context
ALTER TABLE audit_log
ADD COLUMN impersonated_by VARCHAR(255);

ALTER TABLE audit_log
ADD COLUMN impersonation_session_id UUID REFERENCES impersonation_sessions (id);
