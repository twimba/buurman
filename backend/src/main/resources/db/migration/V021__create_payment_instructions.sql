CREATE TABLE payment_instructions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier           VARCHAR(29) NOT NULL,
    team_id              UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    payment_method       VARCHAR(50) NOT NULL,

    bank_name            VARCHAR(255),
    account_holder_name  VARCHAR(255),
    iban                 VARCHAR(34),
    bic_swift            VARCHAR(11),
    account_number       VARCHAR(50),
    routing_number       VARCHAR(50),
    payment_reference    VARCHAR(255),
    additional_details   TEXT,

    is_default           BOOLEAN NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           UUID NOT NULL REFERENCES users(id),
    updated_by           UUID NOT NULL REFERENCES users(id),
    deleted_at           TIMESTAMP,

    CONSTRAINT chk_pi_payment_method CHECK (
        payment_method IN ('BANK_TRANSFER', 'PAYPAL', 'CASH', 'CHECK', 'DIRECT_DEBIT', 'OTHER')
    )
);

CREATE UNIQUE INDEX idx_pi_team_identifier ON payment_instructions(team_id, identifier);
CREATE INDEX idx_pi_team ON payment_instructions(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_pi_deleted_at ON payment_instructions(deleted_at);
CREATE UNIQUE INDEX idx_pi_team_default ON payment_instructions(team_id)
    WHERE deleted_at IS NULL AND is_default = TRUE;
