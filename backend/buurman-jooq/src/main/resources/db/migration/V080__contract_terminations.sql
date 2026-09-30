CREATE TABLE contract_terminations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    given_by VARCHAR(16) NOT NULL,
    notice_date DATE NOT NULL,
    ground_code VARCHAR(64),
    computed_end_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    override_reason TEXT,
    inspection_date DATE,
    notice_letter_document_id UUID REFERENCES documents (id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT uq_contract_terminations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_contract_terminations_contract UNIQUE (contract_id)
);

CREATE INDEX idx_contract_terminations_team ON contract_terminations (team_id);

CREATE INDEX idx_contract_terminations_effective_end_date ON contract_terminations (effective_end_date)
WHERE
    status = 'NOTICE_GIVEN';
