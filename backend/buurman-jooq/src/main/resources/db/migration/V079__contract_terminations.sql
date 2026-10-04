-- ===== from V080__contract_terminations.sql =====
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

-- ===== from V081__termination_notice_rules.sql =====
CREATE TABLE rent_regulation_termination_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    party_type VARCHAR(16) NOT NULL,
    min_tenancy_months INTEGER,
    notice_days INTEGER NOT NULL,
    grounds_required BOOLEAN NOT NULL DEFAULT FALSE,
    grounds_codes TEXT[],
    source_url TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rrt_country ON rent_regulation_termination_rules (country_id);

-- Seed data: notice-period figures explicitly stated in BUUR-105's own ticket
-- text (NL/DE/FR only) — example/starting data, not verified legal advice.
-- A landlord relying on this for an actual termination should confirm
-- current local requirements; this computes from stored data, it doesn't
-- certify the data's correctness.
INSERT INTO
    rent_regulation_termination_rules (
        country_id,
        party_type,
        min_tenancy_months,
        notice_days,
        grounds_required,
        grounds_codes,
        notes
    )
SELECT
    id,
    'LANDLORD',
    0,
    90,
    TRUE,
    ARRAY[
        'OWN_USE',
        'RENOVATION',
        'BREACH',
        'OTHER_LEGAL_GROUND'
    ],
    'BUUR-105 ticket-stated figure (3 months minimum); example data, verify against current NL law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'NL'
UNION ALL
SELECT
    id,
    'LANDLORD',
    0,
    90,
    FALSE,
    NULL,
    'BUUR-105 ticket-stated figure (3 months, <5yr tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'DE'
UNION ALL
SELECT
    id,
    'LANDLORD',
    60,
    180,
    FALSE,
    NULL,
    'BUUR-105 ticket-stated figure (6 months, 5-8yr tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'DE'
UNION ALL
SELECT
    id,
    'LANDLORD',
    96,
    270,
    FALSE,
    NULL,
    'BUUR-105 ticket-stated figure (9 months, 8yr+ tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'DE'
UNION ALL
SELECT
    id,
    'LANDLORD',
    0,
    90,
    FALSE,
    NULL,
    'BUUR-105 ticket-stated figure (3 months); example data, verify against current FR law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'FR'
UNION ALL
SELECT
    id,
    'TENANT',
    0,
    90,
    FALSE,
    NULL,
    'BUUR-105 ticket-stated figure (3 months); example data, verify against current FR law before production use.'
FROM
    rent_regulation_countries
WHERE
    country_code = 'FR';
