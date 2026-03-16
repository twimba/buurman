-- V034__contract_extensions.sql
-- Contract Renewals & Extensions (BUUR-78)
-- NOTE: Old columns (auto_renewal, renewal_notice_days) are kept for now.
--       They will be dropped in V035 after code migration is verified.

-- =====================================================================
-- 1. New renewal config columns on contracts
-- =====================================================================

ALTER TABLE contracts
    ADD COLUMN renewal_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN renewal_term_months INTEGER,
    ADD COLUMN max_renewals INTEGER,
    ADD COLUMN landlord_notice_days INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN tenant_notice_days INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN requires_tenant_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rent_adjustment_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN rent_adjustment_value DECIMAL(15, 4),
    ADD COLUMN landlord_type VARCHAR(20),
    ADD COLUMN region_code VARCHAR(10);

ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_renewal_mode
        CHECK (renewal_mode IN ('NONE', 'AUTOMATIC', 'MANUAL')),
    ADD CONSTRAINT chk_contracts_renewal_term_positive
        CHECK (renewal_term_months IS NULL OR renewal_term_months > 0),
    ADD CONSTRAINT chk_contracts_max_renewals_positive
        CHECK (max_renewals IS NULL OR max_renewals > 0),
    ADD CONSTRAINT chk_contracts_landlord_notice_non_negative
        CHECK (landlord_notice_days >= 0),
    ADD CONSTRAINT chk_contracts_tenant_notice_non_negative
        CHECK (tenant_notice_days >= 0),
    ADD CONSTRAINT chk_contracts_rent_adj_type
        CHECK (rent_adjustment_type IN ('NONE', 'FIXED_PERCENTAGE', 'FIXED_AMOUNT', 'MANUAL')),
    ADD CONSTRAINT chk_contracts_rent_adj_value_required
        CHECK (
            (rent_adjustment_type IN ('NONE', 'MANUAL') AND rent_adjustment_value IS NULL)
            OR (rent_adjustment_type NOT IN ('NONE', 'MANUAL') AND rent_adjustment_value IS NOT NULL)
        ),
    ADD CONSTRAINT chk_contracts_rent_adj_percentage_range
        CHECK (rent_adjustment_type != 'FIXED_PERCENTAGE'
            OR (rent_adjustment_value >= -100 AND rent_adjustment_value <= 100)),
    ADD CONSTRAINT chk_contracts_landlord_type
        CHECK (landlord_type IS NULL OR landlord_type IN ('NATURAL_PERSON', 'LEGAL_ENTITY'));

CREATE INDEX idx_contracts_landlord_type
    ON contracts (team_id, landlord_type) WHERE deleted_at IS NULL AND landlord_type IS NOT NULL;
CREATE INDEX idx_contracts_region_code
    ON contracts (team_id, country_code, region_code) WHERE deleted_at IS NULL AND region_code IS NOT NULL;

-- =====================================================================
-- 2. Data migration: auto_renewal + renewal_notice_days → new columns
-- =====================================================================

UPDATE contracts
SET renewal_mode = 'AUTOMATIC',
    landlord_notice_days = COALESCE(renewal_notice_days, 30),
    tenant_notice_days = COALESCE(renewal_notice_days, 30),
    renewal_term_months = 12
WHERE auto_renewal = TRUE AND deleted_at IS NULL;

UPDATE contracts
SET landlord_notice_days = COALESCE(renewal_notice_days, 30),
    tenant_notice_days = COALESCE(renewal_notice_days, 30)
WHERE (auto_renewal = FALSE OR auto_renewal IS NULL) AND deleted_at IS NULL;

-- =====================================================================
-- 3. Contract extensions table
-- =====================================================================

CREATE TABLE contract_extensions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    extension_number INTEGER NOT NULL,
    previous_end_date DATE NOT NULL,
    new_end_date DATE,
    previous_rent_amount BIGINT NOT NULL,
    previous_rent_currency VARCHAR(3) NOT NULL,
    new_rent_amount BIGINT NOT NULL,
    new_rent_currency VARCHAR(3) NOT NULL,
    rent_adjustment_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    rent_adjustment_value DECIMAL(15, 4),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    trigger_type VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
    rent_period_id UUID REFERENCES contract_rent_periods (id),
    notes TEXT,
    declined_reason TEXT,
    activated_at TIMESTAMP,
    activated_by UUID REFERENCES users (id),
    confirmed_at TIMESTAMP,
    confirmed_by UUID REFERENCES users (id),
    superseded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_extensions_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_extensions_contract_number UNIQUE (contract_id, extension_number),
    CONSTRAINT chk_extensions_number_positive CHECK (extension_number > 0),
    CONSTRAINT chk_extensions_rent_positive CHECK (new_rent_amount > 0 AND previous_rent_amount > 0),
    CONSTRAINT chk_extensions_dates CHECK (new_end_date IS NULL OR new_end_date > previous_end_date),
    CONSTRAINT chk_extensions_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'CANCELLED', 'DECLINED')),
    CONSTRAINT chk_extensions_trigger_type CHECK (trigger_type IN ('MANUAL', 'AUTO')),
    CONSTRAINT chk_extensions_rent_adj_type CHECK (rent_adjustment_type IN ('NONE', 'FIXED_PERCENTAGE', 'FIXED_AMOUNT', 'MANUAL'))
);

CREATE INDEX idx_extensions_team_id ON contract_extensions (team_id);
CREATE INDEX idx_extensions_contract_id ON contract_extensions (contract_id);
CREATE INDEX idx_extensions_status ON contract_extensions (status);
CREATE INDEX idx_extensions_contract_status ON contract_extensions (contract_id, status) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_extensions_contract_active_draft ON contract_extensions (contract_id) WHERE status IN ('ACTIVE', 'DRAFT') AND deleted_at IS NULL;
CREATE INDEX idx_contracts_renewal_candidates ON contracts (team_id, end_date, renewal_mode) WHERE deleted_at IS NULL AND status = 'ACTIVE' AND renewal_mode IN ('AUTOMATIC', 'MANUAL');

-- =====================================================================
-- 4. Jurisdiction defaults table
-- =====================================================================

CREATE TABLE jurisdiction_defaults (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10),
    landlord_type VARCHAR(20),
    furnished BOOLEAN,
    field_name VARCHAR(50) NOT NULL,
    value VARCHAR(100) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_jd_landlord_type CHECK (landlord_type IS NULL OR landlord_type IN ('NATURAL_PERSON', 'LEGAL_ENTITY')),
    CONSTRAINT chk_jd_valid_dates CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT chk_jd_field_name CHECK (field_name IN (
        'landlord_notice_days', 'tenant_notice_days', 'renewal_term_months',
        'mandatory_term_years', 'max_rent_increase_percent', 'notice_method'))
);

CREATE UNIQUE INDEX uq_jd_active ON jurisdiction_defaults
    (country_code, COALESCE(region_code, ''), COALESCE(landlord_type, ''), COALESCE(furnished, FALSE), field_name)
    WHERE valid_until IS NULL;
CREATE INDEX idx_jd_lookup ON jurisdiction_defaults (country_code, field_name, valid_from);

-- =====================================================================
-- 5. Seed jurisdiction defaults (35 rows across 7 countries)
-- =====================================================================

-- NL
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('NL', 'landlord_notice_days', '90', '2020-01-01', 'Art. 7:271 BW — missed notice converts to indefinite'),
    ('NL', 'tenant_notice_days', '30', '2020-01-01', 'Art. 7:271 BW'),
    ('NL', 'renewal_term_months', '12', '2020-01-01', 'Typical yearly renewal');

-- DE
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('DE', 'landlord_notice_days', '90', '2020-01-01', '§ 573c BGB — increases by tenancy length'),
    ('DE', 'tenant_notice_days', '90', '2020-01-01', '§ 573c BGB'),
    ('DE', 'renewal_term_months', '0', '2020-01-01', 'Fixed-term rare, usually indefinite');

-- FR unfurnished
INSERT INTO jurisdiction_defaults (country_code, furnished, field_name, value, valid_from, notes) VALUES
    ('FR', FALSE, 'landlord_notice_days', '180', '2020-01-01', 'Loi du 6 juillet 1989'),
    ('FR', FALSE, 'tenant_notice_days', '90', '2020-01-01', '3 months (1 month in tensioned zones)'),
    ('FR', FALSE, 'renewal_term_months', '36', '2020-01-01', '3-year renewal blocks');

-- FR furnished
INSERT INTO jurisdiction_defaults (country_code, furnished, field_name, value, valid_from, notes) VALUES
    ('FR', TRUE, 'landlord_notice_days', '90', '2020-01-01', 'Loi ALUR'),
    ('FR', TRUE, 'tenant_notice_days', '30', '2020-01-01', '1 month'),
    ('FR', TRUE, 'renewal_term_months', '12', '2020-01-01', '1-year renewal blocks');

-- ES natural person
INSERT INTO jurisdiction_defaults (country_code, landlord_type, field_name, value, valid_from, notes) VALUES
    ('ES', 'NATURAL_PERSON', 'landlord_notice_days', '120', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'NATURAL_PERSON', 'tenant_notice_days', '30', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'NATURAL_PERSON', 'renewal_term_months', '12', '2020-01-01', 'Annual extensions'),
    ('ES', 'NATURAL_PERSON', 'mandatory_term_years', '5', '2020-01-01', 'LAU Art. 9');

-- ES legal entity
INSERT INTO jurisdiction_defaults (country_code, landlord_type, field_name, value, valid_from, notes) VALUES
    ('ES', 'LEGAL_ENTITY', 'landlord_notice_days', '120', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'LEGAL_ENTITY', 'tenant_notice_days', '30', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'LEGAL_ENTITY', 'renewal_term_months', '12', '2020-01-01', 'Annual extensions'),
    ('ES', 'LEGAL_ENTITY', 'mandatory_term_years', '7', '2020-01-01', 'LAU Art. 9');

-- BE Brussels
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'BRU', 'landlord_notice_days', '180', '2020-01-01', 'Brussels Housing Code'),
    ('BE', 'BRU', 'tenant_notice_days', '90', '2020-01-01', 'Brussels Housing Code'),
    ('BE', 'BRU', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- BE Flanders
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'VLG', 'landlord_notice_days', '180', '2020-01-01', 'Vlaams Woninghuurdecreet'),
    ('BE', 'VLG', 'tenant_notice_days', '90', '2020-01-01', 'Vlaams Woninghuurdecreet'),
    ('BE', 'VLG', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- BE Wallonia
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'WAL', 'landlord_notice_days', '180', '2020-01-01', 'Decret wallon relatif au bail'),
    ('BE', 'WAL', 'tenant_notice_days', '90', '2020-01-01', 'Decret wallon relatif au bail'),
    ('BE', 'WAL', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- PT
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('PT', 'landlord_notice_days', '120', '2020-01-01', 'NRAU Art. 1097'),
    ('PT', 'tenant_notice_days', '60', '2020-01-01', 'NRAU Art. 1098'),
    ('PT', 'renewal_term_months', '12', '2020-01-01', 'Typical yearly');

-- AT
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('AT', 'landlord_notice_days', '90', '2020-01-01', 'MRG § 30'),
    ('AT', 'tenant_notice_days', '30', '2020-01-01', 'MRG § 30'),
    ('AT', 'renewal_term_months', '36', '2020-01-01', '3-year standard');
