CREATE TABLE contract_payment_instructions (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier                 VARCHAR(29) NOT NULL,
    team_id                    UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    contract_id                UUID NOT NULL REFERENCES contracts(id),

    payment_instruction_id     UUID REFERENCES payment_instructions(id),
    is_custom                  BOOLEAN NOT NULL DEFAULT FALSE,

    custom_name                VARCHAR(255),
    custom_description         TEXT,
    custom_payment_method      VARCHAR(50),
    custom_bank_name           VARCHAR(255),
    custom_account_holder_name VARCHAR(255),
    custom_iban                VARCHAR(34),
    custom_bic_swift           VARCHAR(11),
    custom_account_number      VARCHAR(50),
    custom_routing_number      VARCHAR(50),
    custom_payment_reference   VARCHAR(255),
    custom_additional_details  TEXT,

    effective_from             DATE NOT NULL,
    effective_to               DATE,
    notes                      TEXT,

    created_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 UUID NOT NULL REFERENCES users(id),
    updated_by                 UUID NOT NULL REFERENCES users(id),
    deleted_at                 TIMESTAMP,

    CONSTRAINT chk_cpi_custom_method CHECK (
        custom_payment_method IS NULL OR
        custom_payment_method IN ('BANK_TRANSFER', 'PAYPAL', 'CASH', 'CHECK', 'DIRECT_DEBIT', 'OTHER')
    ),
    CONSTRAINT chk_cpi_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT chk_cpi_source CHECK (
        (is_custom = TRUE) OR (payment_instruction_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_cpi_team_identifier ON contract_payment_instructions(team_id, identifier);
CREATE INDEX idx_cpi_contract ON contract_payment_instructions(team_id, contract_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cpi_template ON contract_payment_instructions(payment_instruction_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cpi_deleted_at ON contract_payment_instructions(deleted_at);
