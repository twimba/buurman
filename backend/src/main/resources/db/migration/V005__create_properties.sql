CREATE TABLE properties (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier                  VARCHAR(29) NOT NULL,
    team_id                     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    -- Address
    street                      VARCHAR(255) NOT NULL,
    city                        VARCHAR(100) NOT NULL,
    postal_code                 VARCHAR(20) NOT NULL,
    country                     VARCHAR(100) NOT NULL DEFAULT 'Netherlands',
    latitude                    DECIMAL(10, 8),
    longitude                   DECIMAL(11, 8),

    -- Specifications
    bedrooms                    INTEGER,
    bathrooms                   INTEGER,
    area_value                  NUMERIC(10,2),
    area_unit                   VARCHAR(10) NOT NULL DEFAULT 'sqm',
    property_type               VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'VACANT',

    -- Construction & Structure
    year_built                  INTEGER,
    year_last_renovated         INTEGER,
    construction_type           VARCHAR(50),
    foundation_type             VARCHAR(50),
    roof_type                   VARCHAR(50),
    wall_construction           VARCHAR(50),
    flooring_type               VARCHAR(50),
    window_type                 VARCHAR(50),
    number_of_floors            INTEGER,
    structural_notes            TEXT,

    -- Energy & Climate
    energy_efficiency_rating    VARCHAR(5),
    energy_certificate_expiry_date DATE,
    heating_type                VARCHAR(50),
    cooling_type                VARCHAR(50),
    hot_water_system            VARCHAR(50),
    insulation_notes            TEXT,

    -- Utilities & Connections
    electricity_connection_type VARCHAR(50),
    electricity_capacity_amps   INTEGER,
    water_connection_type       VARCHAR(50),
    has_gas_connection          BOOLEAN DEFAULT FALSE,
    sewage_type                 VARCHAR(50),
    internet_connection_type    VARCHAR(50),
    internet_max_speed_mbps     INTEGER,
    internet_status             VARCHAR(50),

    -- Parking
    parking_spaces              INTEGER,
    parking_type                VARCHAR(50),

    -- Safety & Security
    has_smoke_detectors         BOOLEAN DEFAULT FALSE,
    has_co_detectors            BOOLEAN DEFAULT FALSE,
    has_fire_extinguisher       BOOLEAN DEFAULT FALSE,
    has_sprinkler_system        BOOLEAN DEFAULT FALSE,
    has_alarm_system            BOOLEAN DEFAULT FALSE,
    has_security_cameras        BOOLEAN DEFAULT FALSE,
    has_secure_entry            BOOLEAN DEFAULT FALSE,
    safety_notes                TEXT,

    -- Accessibility
    is_wheelchair_accessible    BOOLEAN DEFAULT FALSE,
    has_elevator                BOOLEAN DEFAULT FALSE,
    has_step_free_entrance      BOOLEAN DEFAULT FALSE,
    has_adapted_bathroom        BOOLEAN DEFAULT FALSE,
    accessibility_notes         TEXT,

    -- Audit
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by                  UUID REFERENCES users(id),
    updated_by                  UUID REFERENCES users(id),
    deleted_at                  TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_properties_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_properties_property_type CHECK (property_type IN ('APARTMENT', 'HOUSE', 'STUDIO', 'COMMERCIAL')),
    CONSTRAINT chk_properties_status CHECK (status IN ('VACANT', 'OCCUPIED', 'MAINTENANCE', 'UNAVAILABLE')),
    CONSTRAINT chk_properties_bedrooms CHECK (bedrooms >= 0),
    CONSTRAINT chk_properties_bathrooms CHECK (bathrooms >= 0),
    CONSTRAINT chk_properties_area_value CHECK (area_value > 0),
    CONSTRAINT chk_properties_area_unit CHECK (area_unit IN ('sqm', 'sqft')),
    CONSTRAINT chk_properties_year_built CHECK (year_built >= 1600 AND year_built <= 2100),
    CONSTRAINT chk_properties_year_last_renovated CHECK (year_last_renovated >= 1600 AND year_last_renovated <= 2100),
    CONSTRAINT chk_properties_construction_type CHECK (construction_type IN ('BRICK', 'CONCRETE', 'WOOD', 'STEEL', 'MIXED', 'OTHER')),
    CONSTRAINT chk_properties_foundation_type CHECK (foundation_type IN ('CONCRETE_SLAB', 'CRAWL_SPACE', 'BASEMENT', 'PILE', 'OTHER')),
    CONSTRAINT chk_properties_roof_type CHECK (roof_type IN ('FLAT', 'PITCHED', 'HIP', 'GABLE', 'MANSARD', 'OTHER')),
    CONSTRAINT chk_properties_flooring_type CHECK (flooring_type IN ('HARDWOOD', 'LAMINATE', 'TILE', 'VINYL', 'CARPET', 'CONCRETE', 'MIXED', 'OTHER')),
    CONSTRAINT chk_properties_window_type CHECK (window_type IN ('SINGLE_PANE', 'DOUBLE_PANE', 'TRIPLE_PANE', 'OTHER')),
    CONSTRAINT chk_properties_number_of_floors CHECK (number_of_floors >= 1),
    CONSTRAINT chk_properties_heating_type CHECK (heating_type IN ('CENTRAL', 'DISTRICT', 'HEAT_PUMP', 'ELECTRIC', 'GAS', 'NONE', 'OTHER')),
    CONSTRAINT chk_properties_cooling_type CHECK (cooling_type IN ('CENTRAL_AC', 'SPLIT_AC', 'EVAPORATIVE', 'NONE', 'OTHER')),
    CONSTRAINT chk_properties_hot_water_system CHECK (hot_water_system IN ('BOILER', 'TANKLESS', 'HEAT_PUMP', 'SOLAR', 'ELECTRIC', 'OTHER')),
    CONSTRAINT chk_properties_electricity_capacity_amps CHECK (electricity_capacity_amps > 0),
    CONSTRAINT chk_properties_sewage_type CHECK (sewage_type IN ('MUNICIPAL', 'SEPTIC', 'OTHER')),
    CONSTRAINT chk_properties_internet_connection_type CHECK (internet_connection_type IN ('FIBER', 'CABLE', 'DSL', 'NONE', 'OTHER')),
    CONSTRAINT chk_properties_internet_max_speed_mbps CHECK (internet_max_speed_mbps > 0),
    CONSTRAINT chk_properties_internet_status CHECK (internet_status IN ('ACTIVE', 'AVAILABLE', 'NOT_AVAILABLE', 'UNKNOWN')),
    CONSTRAINT chk_properties_parking_spaces CHECK (parking_spaces >= 0),
    CONSTRAINT chk_properties_parking_type CHECK (parking_type IN ('GARAGE', 'CARPORT', 'DRIVEWAY', 'STREET', 'UNDERGROUND', 'NONE', 'OTHER'))
);

CREATE INDEX idx_properties_team ON properties(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_status ON properties(team_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_identifier ON properties(team_id, identifier);
CREATE INDEX idx_properties_coordinates ON properties(latitude, longitude) WHERE deleted_at IS NULL;
