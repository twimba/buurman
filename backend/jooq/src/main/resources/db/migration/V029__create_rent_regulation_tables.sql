-- Rent regulation reference tables (global, no team_id)
-- Used by Rent Updates Central (BUUR-35) to provide country/region-specific rent increase rules
CREATE TABLE rent_regulation_countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL UNIQUE,
    country_name VARCHAR(100) NOT NULL,
    has_regional_regulations BOOLEAN DEFAULT FALSE,
    summary TEXT,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE rent_regulation_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_code VARCHAR(20) NOT NULL,
    region_name VARCHAR(100) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, region_code),
    UNIQUE (country_id, identifier)
);

CREATE TABLE rent_regulation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    YEAR INT NOT NULL,
    property_category VARCHAR(50) DEFAULT 'ALL',
    sector VARCHAR(50),
    max_increase_percentage DECIMAL(5, 2),
    max_increase_type VARCHAR(30) NOT NULL,
    index_name VARCHAR(100),
    index_value DECIMAL(10, 4),
    effective_date DATE,
    notice_period_days INT,
    frequency VARCHAR(20) DEFAULT 'ANNUAL',
    additional_conditions TEXT,
    source_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, identifier)
);

CREATE INDEX idx_rent_reg_rules_country_year ON rent_regulation_rules (country_id, YEAR);

CREATE INDEX idx_rent_reg_rules_region ON rent_regulation_rules (region_id);
