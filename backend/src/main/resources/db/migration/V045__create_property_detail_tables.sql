-- Residential details
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
    CONSTRAINT chk_residential_bathrooms CHECK (bathrooms >= 0),
    CONSTRAINT chk_residential_pet_policy CHECK (
        pet_policy IN ('ALLOWED', 'NOT_ALLOWED', 'NEGOTIABLE')
    )
);

CREATE INDEX idx_residential_details_team ON property_residential_details (team_id);

CREATE INDEX idx_residential_details_property ON property_residential_details (property_id);

-- Migrate existing bedrooms/bathrooms data
INSERT INTO
    property_residential_details (
        property_id,
        team_id,
        bedrooms,
        bathrooms,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    id,
    team_id,
    bedrooms,
    bathrooms,
    created_at,
    updated_at,
    created_by,
    updated_by
FROM
    properties
WHERE
    property_category = 'RESIDENTIAL'
    AND deleted_at IS NULL;

-- Drop bedrooms/bathrooms from properties
ALTER TABLE properties
DROP CONSTRAINT IF EXISTS chk_properties_bedrooms;

ALTER TABLE properties
DROP CONSTRAINT IF EXISTS chk_properties_bathrooms;

ALTER TABLE properties
DROP COLUMN bedrooms;

ALTER TABLE properties
DROP COLUMN bathrooms;

-- Commercial details
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
    CONSTRAINT chk_commercial_usable_area_unit CHECK (usable_area_unit IN ('sqm', 'sqft')),
    CONSTRAINT chk_commercial_common_area CHECK (common_area_value > 0),
    CONSTRAINT chk_commercial_common_area_unit CHECK (common_area_unit IN ('sqm', 'sqft')),
    CONSTRAINT chk_commercial_ceiling_height CHECK (ceiling_height_m > 0),
    CONSTRAINT chk_commercial_max_occupancy CHECK (max_occupancy > 0),
    CONSTRAINT chk_commercial_restroom_count CHECK (restroom_count >= 0)
);

CREATE INDEX idx_commercial_details_team ON property_commercial_details (team_id);

CREATE INDEX idx_commercial_details_property ON property_commercial_details (property_id);

-- Industrial details
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
    CONSTRAINT chk_industrial_yard_area CHECK (yard_area_value > 0),
    CONSTRAINT chk_industrial_yard_area_unit CHECK (yard_area_unit IN ('sqm', 'sqft'))
);

CREATE INDEX idx_industrial_details_team ON property_industrial_details (team_id);

CREATE INDEX idx_industrial_details_property ON property_industrial_details (property_id);

-- Agricultural details
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
    CONSTRAINT chk_agricultural_land_area_unit CHECK (
        total_land_area_unit IN ('hectares', 'acres', 'sqm')
    ),
    CONSTRAINT chk_agricultural_arable_area CHECK (arable_area_value > 0),
    CONSTRAINT chk_agricultural_arable_area_unit CHECK (arable_area_unit IN ('hectares', 'acres', 'sqm')),
    CONSTRAINT chk_agricultural_soil_type CHECK (
        soil_type IN (
            'CLAY',
            'SANDY',
            'LOAM',
            'PEAT',
            'CHALK',
            'SILT',
            'OTHER'
        )
    ),
    CONSTRAINT chk_agricultural_water_source CHECK (
        water_source IN (
            'WELL',
            'RIVER',
            'CANAL',
            'MUNICIPAL',
            'RAINWATER',
            'NONE'
        )
    ),
    CONSTRAINT chk_agricultural_irrigation CHECK (
        irrigation_type IN ('DRIP', 'SPRINKLER', 'FLOOD', 'PIVOT', 'NONE')
    ),
    CONSTRAINT chk_agricultural_fencing CHECK (
        fencing_type IN (
            'WIRE',
            'ELECTRIC',
            'WOODEN',
            'STONE_WALL',
            'NONE'
        )
    )
);

CREATE INDEX idx_agricultural_details_team ON property_agricultural_details (team_id);

CREATE INDEX idx_agricultural_details_property ON property_agricultural_details (property_id);
