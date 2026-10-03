ALTER TABLE lease_clause_templates
ADD COLUMN lease_kind VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing V083 rows are placeholder text: keep them as the fallback for every country/kind
-- that has no real document yet.
UPDATE lease_clause_templates
SET
    lease_kind = 'LEGACY';

ALTER TABLE lease_clause_templates
ADD CONSTRAINT chk_lease_clause_templates_kind CHECK (
    lease_kind IN (
        'LEGACY',
        'RESIDENTIAL',
        'RESIDENTIAL_FURNISHED',
        'COMMERCIAL',
        'MIXED_USE',
        'AGRICULTURAL',
        'SHORT_TERM',
        'STUDENT_MOBILITY'
    )
);

ALTER TABLE lease_clause_templates
DROP CONSTRAINT uq_lease_clause_templates_country_key_version;

ALTER TABLE lease_clause_templates
ADD CONSTRAINT uq_lease_clause_templates_country_kind_key_version UNIQUE (country_code, lease_kind, clause_key, version);

-- V090's partial index must also become kind-aware, otherwise the same clause_key could not be
-- active under two different kinds of one country.
DROP INDEX uq_lease_clause_templates_active_country_key;

CREATE UNIQUE INDEX uq_lease_clause_templates_active_country_kind_key ON lease_clause_templates (country_code, lease_kind, clause_key)
WHERE
    deleted_at IS NULL;

ALTER TABLE contracts
ADD COLUMN lease_regime VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
ADD CONSTRAINT chk_contracts_lease_regime CHECK (
    lease_regime IN ('STANDARD', 'SHORT_TERM', 'STUDENT_OR_MOBILITY')
);
