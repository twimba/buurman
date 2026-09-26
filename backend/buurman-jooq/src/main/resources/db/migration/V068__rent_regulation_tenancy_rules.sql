-- Display-only tenancy-law reference entries per regulation country.
-- Facts that are not rent-increase caps: notice periods, tenancy duration, deposits,
-- lease formalities, registration duties, fixed-amount fees.
-- Global reference data: no team_id, no deleted_at — reload wipes and re-seeds,
-- matching rent_regulation_regions.
CREATE TABLE rent_regulation_tenancy_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    topic VARCHAR(40) NOT NULL,
    label VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    effective_from DATE,
    legal_basis TEXT,
    source_url TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, identifier),
    CONSTRAINT chk_rr_tenancy_rules_label_not_blank CHECK (btrim(label) <> ''),
    CONSTRAINT chk_rr_tenancy_rules_value_not_blank CHECK (btrim(value) <> '')
);

CREATE INDEX idx_rr_tenancy_rules_country ON rent_regulation_tenancy_rules (country_id);
CREATE INDEX idx_rr_tenancy_rules_topic ON rent_regulation_tenancy_rules (country_id, topic);
CREATE INDEX idx_rr_tenancy_rules_region ON rent_regulation_tenancy_rules (region_id);
