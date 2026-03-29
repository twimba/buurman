-- =============================================================================
-- 1. properties
-- =============================================================================
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Category & Type
    property_category VARCHAR(50) NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VACANT',
    -- Address
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Netherlands',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    geocode_accuracy VARCHAR(30),
    -- Specifications
    area_value NUMERIC(10, 2),
    area_unit VARCHAR(10) NOT NULL DEFAULT 'sqm',
    -- Construction & Structure
    year_built INTEGER,
    year_last_renovated INTEGER,
    construction_type VARCHAR(50),
    foundation_type VARCHAR(50),
    roof_type VARCHAR(50),
    wall_construction VARCHAR(50),
    flooring_type VARCHAR(50),
    window_type VARCHAR(50),
    number_of_floors INTEGER,
    structural_notes TEXT,
    -- Energy & Climate
    energy_efficiency_rating VARCHAR(5),
    energy_certificate_expiry_date DATE,
    heating_type VARCHAR(50),
    cooling_type VARCHAR(50),
    hot_water_system VARCHAR(50),
    insulation_notes TEXT,
    -- Utilities & Connections
    electricity_connection_type VARCHAR(50),
    electricity_capacity_amps INTEGER,
    water_connection_type VARCHAR(50),
    has_gas_connection BOOLEAN DEFAULT FALSE,
    sewage_type VARCHAR(50),
    internet_connection_type VARCHAR(50),
    internet_max_speed_mbps INTEGER,
    internet_status VARCHAR(50),
    -- Parking
    parking_spaces INTEGER,
    parking_type VARCHAR(50),
    -- Safety & Security
    has_smoke_detectors BOOLEAN DEFAULT FALSE,
    has_co_detectors BOOLEAN DEFAULT FALSE,
    has_fire_extinguisher BOOLEAN DEFAULT FALSE,
    has_sprinkler_system BOOLEAN DEFAULT FALSE,
    has_alarm_system BOOLEAN DEFAULT FALSE,
    has_security_cameras BOOLEAN DEFAULT FALSE,
    has_secure_entry BOOLEAN DEFAULT FALSE,
    safety_notes TEXT,
    -- Accessibility
    is_wheelchair_accessible BOOLEAN DEFAULT FALSE,
    has_elevator BOOLEAN DEFAULT FALSE,
    has_step_free_entrance BOOLEAN DEFAULT FALSE,
    has_adapted_bathroom BOOLEAN DEFAULT FALSE,
    accessibility_notes TEXT,
    -- Acquisition & Valuation
    purchase_price BIGINT,
    purchase_price_currency VARCHAR(3),
    purchase_date DATE,
    current_market_value BIGINT,
    current_market_value_currency VARCHAR(3),
    market_value_date DATE,
    -- Mortgage / Financing
    mortgage_type VARCHAR(30),
    mortgage_amount BIGINT,
    mortgage_amount_currency VARCHAR(3),
    mortgage_interest_rate DECIMAL(5, 3),
    mortgage_start_date DATE,
    mortgage_end_date DATE,
    monthly_mortgage_payment BIGINT,
    monthly_mortgage_payment_currency VARCHAR(3),
    -- Operating Costs (annual, stored in minor units)
    annual_property_tax BIGINT,
    annual_property_tax_currency VARCHAR(3),
    annual_property_tax_due_month VARCHAR(50),
    annual_insurance BIGINT,
    annual_insurance_currency VARCHAR(3),
    annual_insurance_due_month VARCHAR(50),
    annual_hoa_fee BIGINT,
    annual_hoa_fee_currency VARCHAR(3),
    annual_hoa_fee_due_month VARCHAR(50),
    annual_management_fee BIGINT,
    annual_management_fee_currency VARCHAR(3),
    annual_management_fee_due_month VARCHAR(50),
    annual_maintenance_reserve BIGINT,
    annual_maintenance_reserve_currency VARCHAR(3),
    annual_maintenance_reserve_due_month VARCHAR(50),
    -- Depreciation
    depreciation_method VARCHAR(30),
    depreciation_years INTEGER,
    land_value BIGINT,
    land_value_currency VARCHAR(3),
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_properties_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_properties_area_value CHECK (area_value > 0),
    CONSTRAINT chk_properties_number_of_floors CHECK (number_of_floors >= 1),
    CONSTRAINT chk_properties_electricity_capacity_amps CHECK (electricity_capacity_amps > 0),
    CONSTRAINT chk_properties_internet_max_speed_mbps CHECK (internet_max_speed_mbps > 0),
    CONSTRAINT chk_properties_parking_spaces CHECK (parking_spaces >= 0),
    -- Financial value constraints (V051)
    CONSTRAINT chk_purchase_price_positive CHECK (
        purchase_price IS NULL
        OR purchase_price > 0
    ),
    CONSTRAINT chk_current_market_value_positive CHECK (
        current_market_value IS NULL
        OR current_market_value > 0
    ),
    CONSTRAINT chk_mortgage_amount_positive CHECK (
        mortgage_amount IS NULL
        OR mortgage_amount > 0
    ),
    CONSTRAINT chk_mortgage_interest_rate_range CHECK (
        mortgage_interest_rate IS NULL
        OR (
            mortgage_interest_rate >= 0
            AND mortgage_interest_rate <= 100
        )
    ),
    CONSTRAINT chk_depreciation_years_positive CHECK (
        depreciation_years IS NULL
        OR depreciation_years > 0
    ),
    CONSTRAINT chk_land_value_non_negative CHECK (
        land_value IS NULL
        OR land_value >= 0
    ),
    -- Operating cost constraints (V052)
    CONSTRAINT chk_annual_property_tax_non_negative CHECK (
        annual_property_tax IS NULL
        OR annual_property_tax >= 0
    ),
    CONSTRAINT chk_annual_insurance_non_negative CHECK (
        annual_insurance IS NULL
        OR annual_insurance >= 0
    ),
    CONSTRAINT chk_annual_hoa_fee_non_negative CHECK (
        annual_hoa_fee IS NULL
        OR annual_hoa_fee >= 0
    ),
    CONSTRAINT chk_annual_management_fee_non_negative CHECK (
        annual_management_fee IS NULL
        OR annual_management_fee >= 0
    ),
    CONSTRAINT chk_annual_maintenance_reserve_non_negative CHECK (
        annual_maintenance_reserve IS NULL
        OR annual_maintenance_reserve >= 0
    ),
    -- Mortgage payment constraint (V061 — allows -1 sentinel for variable payments)
    CONSTRAINT chk_monthly_mortgage_payment_valid CHECK (
        monthly_mortgage_payment IS NULL
        OR monthly_mortgage_payment > 0
        OR monthly_mortgage_payment = -1
    ),
    -- Mortgage date ordering (V052)
    CONSTRAINT chk_mortgage_dates_valid CHECK (
        mortgage_start_date IS NULL
        OR mortgage_end_date IS NULL
        OR mortgage_end_date > mortgage_start_date
    )
);

CREATE INDEX idx_properties_team ON properties (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_properties_status ON properties (team_id, status)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_properties_identifier ON properties (team_id, identifier);

CREATE INDEX idx_properties_coordinates ON properties (latitude, longitude)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_properties_category ON properties (team_id, property_category)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 2. property_outdoor_areas
-- =============================================================================
CREATE TABLE property_outdoor_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    area_value NUMERIC(10, 2),
    area_unit VARCHAR(10) NOT NULL DEFAULT 'sqm',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_outdoor_areas_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_outdoor_areas_area_value CHECK (area_value > 0)
);

CREATE INDEX idx_property_outdoor_areas_team ON property_outdoor_areas (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_property_outdoor_areas_team_property ON property_outdoor_areas (team_id, property_id)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 3. amenities (global lookup table)
-- =============================================================================
CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    applicable_categories VARCHAR(50) [],
    CONSTRAINT amenities_identifier_key UNIQUE (identifier)
);

-- Seed amenities (V017 originals + V046 additions)
INSERT INTO
    amenities (
        identifier,
        name,
        category,
        icon,
        applicable_categories
    )
VALUES
    -- Original amenities (V017)
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Swimming Pool',
        'LEISURE',
        'swimming_pool',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Gym',
        'LEISURE',
        'gym',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Sauna',
        'LEISURE',
        'sauna',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Fireplace',
        'COMFORT',
        'fireplace',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Built-in Vacuum',
        'COMFORT',
        'built_in_vacuum',
        '{RESIDENTIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'In-unit Laundry',
        'APPLIANCE',
        'in_unit_laundry',
        '{RESIDENTIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Shared Laundry',
        'APPLIANCE',
        'shared_laundry',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Dishwasher',
        'APPLIANCE',
        'dishwasher',
        '{RESIDENTIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Storage Unit',
        'STORAGE',
        'storage_unit',
        '{RESIDENTIAL,COMMERCIAL,INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Bike Storage',
        'STORAGE',
        'bike_storage',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Furnished',
        'COMFORT',
        'furnished',
        '{RESIDENTIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Doorman / Concierge',
        'SERVICE',
        'doorman_concierge',
        '{RESIDENTIAL,COMMERCIAL}'
    ),
    -- Category-specific amenities (V046)
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Conference Room',
        'COMFORT',
        'conference_room',
        '{COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Reception Area',
        'COMFORT',
        'reception_area',
        '{COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Server Room',
        'APPLIANCE',
        'server_room',
        '{COMMERCIAL,INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Kitchenette',
        'APPLIANCE',
        'kitchenette',
        '{COMMERCIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Loading Bay',
        'SERVICE',
        'loading_bay',
        '{COMMERCIAL,INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Compressed Air System',
        'APPLIANCE',
        'compressed_air',
        '{INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Chemical Storage',
        'STORAGE',
        'chemical_storage',
        '{INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Overhead Crane',
        'APPLIANCE',
        'overhead_crane',
        '{INDUSTRIAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Irrigation System',
        'SERVICE',
        'irrigation_system',
        '{AGRICULTURAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Grain Silo',
        'STORAGE',
        'grain_silo',
        '{AGRICULTURAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Livestock Shelter',
        'SERVICE',
        'livestock_shelter',
        '{AGRICULTURAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Cold Storage',
        'STORAGE',
        'cold_storage',
        '{INDUSTRIAL,AGRICULTURAL}'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Security Booth',
        'SERVICE',
        'security_booth',
        '{COMMERCIAL,INDUSTRIAL}'
    );

-- =============================================================================
-- 4. property_amenities (join table, team-scoped)
-- =============================================================================
CREATE TABLE property_amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_property_amenities UNIQUE (property_id, amenity_id, team_id)
);

CREATE INDEX idx_property_amenities_team ON property_amenities (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_property_amenities_team_property ON property_amenities (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_property_amenities_amenity ON property_amenities (amenity_id)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 5. property_residential_details
-- =============================================================================
CREATE TABLE property_residential_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    bedrooms INTEGER,
    bathrooms INTEGER,
    furnished BOOLEAN NOT NULL DEFAULT FALSE,
    pet_policy VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_residential_details_property UNIQUE (property_id, team_id),
    CONSTRAINT chk_residential_bedrooms CHECK (bedrooms >= 0),
    CONSTRAINT chk_residential_bathrooms CHECK (bathrooms >= 0)
);

CREATE INDEX idx_residential_details_team ON property_residential_details (team_id);

CREATE INDEX idx_residential_details_property ON property_residential_details (property_id);

-- =============================================================================
-- 6. property_commercial_details
-- =============================================================================
CREATE TABLE property_commercial_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    usable_area_value NUMERIC(10, 2),
    usable_area_unit VARCHAR(10),
    common_area_value NUMERIC(10, 2),
    common_area_unit VARCHAR(10),
    floor_level INTEGER,
    ceiling_height_m NUMERIC(5, 2),
    has_storefront BOOLEAN NOT NULL DEFAULT FALSE,
    has_signage_rights BOOLEAN NOT NULL DEFAULT FALSE,
    zoning_classification VARCHAR(100),
    max_occupancy INTEGER,
    restroom_count INTEGER,
    has_kitchen_facility BOOLEAN NOT NULL DEFAULT FALSE,
    accessibility_compliant BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_commercial_details_property UNIQUE (property_id, team_id),
    CONSTRAINT chk_commercial_usable_area CHECK (usable_area_value > 0),
    CONSTRAINT chk_commercial_common_area CHECK (common_area_value > 0),
    CONSTRAINT chk_commercial_ceiling_height CHECK (ceiling_height_m > 0),
    CONSTRAINT chk_commercial_max_occupancy CHECK (max_occupancy > 0),
    CONSTRAINT chk_commercial_restroom_count CHECK (restroom_count >= 0)
);

CREATE INDEX idx_commercial_details_team ON property_commercial_details (team_id);

CREATE INDEX idx_commercial_details_property ON property_commercial_details (property_id);

-- =============================================================================
-- 7. property_industrial_details
-- =============================================================================
CREATE TABLE property_industrial_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    clear_height_m NUMERIC(5, 2),
    loading_docks INTEGER,
    drive_in_doors INTEGER,
    floor_load_capacity_kg_sqm NUMERIC(10, 2),
    power_capacity_kva INTEGER,
    has_three_phase_power BOOLEAN NOT NULL DEFAULT FALSE,
    has_crane BOOLEAN NOT NULL DEFAULT FALSE,
    crane_capacity_tons NUMERIC(8, 2),
    has_hazmat_certification BOOLEAN NOT NULL DEFAULT FALSE,
    has_ventilation_system BOOLEAN NOT NULL DEFAULT FALSE,
    has_climate_control BOOLEAN NOT NULL DEFAULT FALSE,
    yard_area_value NUMERIC(10, 2),
    yard_area_unit VARCHAR(10),
    zoning_classification VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_industrial_details_property UNIQUE (property_id, team_id),
    CONSTRAINT chk_industrial_clear_height CHECK (clear_height_m > 0),
    CONSTRAINT chk_industrial_loading_docks CHECK (loading_docks >= 0),
    CONSTRAINT chk_industrial_drive_in_doors CHECK (drive_in_doors >= 0),
    CONSTRAINT chk_industrial_floor_load CHECK (floor_load_capacity_kg_sqm > 0),
    CONSTRAINT chk_industrial_power CHECK (power_capacity_kva > 0),
    CONSTRAINT chk_industrial_crane CHECK (crane_capacity_tons > 0),
    CONSTRAINT chk_industrial_yard_area CHECK (yard_area_value > 0)
);

CREATE INDEX idx_industrial_details_team ON property_industrial_details (team_id);

CREATE INDEX idx_industrial_details_property ON property_industrial_details (property_id);

-- =============================================================================
-- 8. property_agricultural_details
-- =============================================================================
CREATE TABLE property_agricultural_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    total_land_area_value NUMERIC(12, 2),
    total_land_area_unit VARCHAR(10),
    arable_area_value NUMERIC(12, 2),
    arable_area_unit VARCHAR(10),
    soil_type VARCHAR(50),
    has_water_rights BOOLEAN NOT NULL DEFAULT FALSE,
    water_source VARCHAR(50),
    irrigation_type VARCHAR(50),
    fencing_type VARCHAR(50),
    has_outbuildings BOOLEAN NOT NULL DEFAULT FALSE,
    outbuilding_details TEXT,
    current_use VARCHAR(100),
    zoning_classification VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_agricultural_details_property UNIQUE (property_id, team_id),
    CONSTRAINT chk_agricultural_land_area CHECK (total_land_area_value > 0),
    CONSTRAINT chk_agricultural_arable_area CHECK (arable_area_value > 0)
);

CREATE INDEX idx_agricultural_details_team ON property_agricultural_details (team_id);

CREATE INDEX idx_agricultural_details_property ON property_agricultural_details (property_id);
