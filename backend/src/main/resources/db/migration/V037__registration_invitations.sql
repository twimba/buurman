-- Registration invitations: platform-level invitation codes for gating user registration
-- Managed by backoffice admins (buurmies), not team-scoped
CREATE TABLE registration_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    code VARCHAR(60) NOT NULL,
    max_usages INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(200) NOT NULL,
    CONSTRAINT uq_reg_invitation_code UNIQUE (code),
    CONSTRAINT uq_reg_invitation_identifier UNIQUE (identifier),
    CONSTRAINT chk_reg_invitation_max_usages CHECK (
        max_usages IS NULL
        OR max_usages > 0
    ),
    CONSTRAINT chk_reg_invitation_usage CHECK (usage_count >= 0)
);

CREATE TABLE registration_invitation_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL REFERENCES registration_invitations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    used_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reg_inv_code ON registration_invitations (code);

CREATE INDEX idx_reg_inv_created ON registration_invitations (created_at DESC);

CREATE INDEX idx_reg_inv_usage_inv ON registration_invitation_usages (invitation_id);

CREATE INDEX idx_reg_inv_usage_user ON registration_invitation_usages (user_id);
