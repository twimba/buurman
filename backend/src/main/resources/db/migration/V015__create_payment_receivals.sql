CREATE TABLE payment_receivals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier      VARCHAR(29) NOT NULL,
    team_id         UUID NOT NULL REFERENCES teams(id),
    payment_id      UUID NOT NULL REFERENCES payments(id),

    -- Financial details
    amount          DECIMAL(10, 2) NOT NULL,
    receival_date   DATE NOT NULL,

    -- Additional details
    notes           TEXT,

    -- Audit
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      UUID NOT NULL REFERENCES users(id),
    updated_by      UUID NOT NULL REFERENCES users(id),
    deleted_at      TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_receivals_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_receivals_amount CHECK (amount > 0)
);

CREATE INDEX idx_payment_receivals_team_id ON payment_receivals(team_id);
CREATE INDEX idx_payment_receivals_payment_id ON payment_receivals(payment_id);
CREATE INDEX idx_payment_receivals_receival_date ON payment_receivals(receival_date);
CREATE INDEX idx_payment_receivals_deleted_at ON payment_receivals(deleted_at);
