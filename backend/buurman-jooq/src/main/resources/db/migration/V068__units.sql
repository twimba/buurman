-- =============================================================================
-- V068__units.sql
-- Introduce a Unit entity under Property (BUUR-106).
-- Property becomes the building; Unit becomes the dwelling.
-- Every existing property is backfilled with one implicit unit, so unit_id can
-- be NOT NULL on contracts, occupancy periods and WWS calculations.
-- =============================================================================
-- 1. units
-- =============================================================================
CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties (id),
    -- Identity within the building
    name VARCHAR(255),
    unit_number VARCHAR(50) NOT NULL,
    floor INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    unit_type VARCHAR(30) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VACANT',
    is_implicit BOOLEAN NOT NULL DEFAULT FALSE,
    -- Valuation & allocation shares
    woz_value BIGINT,
    woz_value_currency VARCHAR(3),
    woz_share_pct NUMERIC(6, 3),
    allocation_share NUMERIC(6, 3),
    -- Dwelling: specifications
    area_value NUMERIC(10, 2),
    area_unit VARCHAR(10) NOT NULL DEFAULT 'sqm',
    -- Dwelling: energy & climate
    energy_efficiency_rating VARCHAR(5),
    energy_certificate_expiry_date DATE,
    heating_type VARCHAR(50),
    cooling_type VARCHAR(50),
    hot_water_system VARCHAR(50),
    insulation_notes TEXT,
    -- Dwelling: finishes
    flooring_type VARCHAR(50),
    window_type VARCHAR(50),
    -- Dwelling: safety
    has_smoke_detectors BOOLEAN DEFAULT FALSE,
    has_co_detectors BOOLEAN DEFAULT FALSE,
    has_fire_extinguisher BOOLEAN DEFAULT FALSE,
    -- Dwelling: accessibility
    has_adapted_bathroom BOOLEAN DEFAULT FALSE,
    accessibility_notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_units_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_units_type CHECK (
        unit_type IN ('APARTMENT', 'PARKING', 'STORAGE', 'COMMERCIAL')
    ),
    CONSTRAINT chk_units_area_positive CHECK (
        area_value IS NULL
        OR area_value > 0
    ),
    CONSTRAINT chk_units_woz_share CHECK (
        woz_share_pct IS NULL
        OR (woz_share_pct >= 0 AND woz_share_pct <= 100)
    ),
    CONSTRAINT chk_units_allocation_share CHECK (
        allocation_share IS NULL
        OR (allocation_share >= 0 AND allocation_share <= 100)
    )
);

CREATE UNIQUE INDEX uq_units_property_number ON units (property_id, unit_number)
WHERE
    (deleted_at IS NULL);

CREATE INDEX idx_units_team_id ON units (team_id);

CREATE INDEX idx_units_property_id ON units (property_id);

CREATE INDEX idx_units_status ON units (team_id, status)
WHERE
    (deleted_at IS NULL);

-- =============================================================================
-- 2. unit_residential_details
-- =============================================================================
CREATE TABLE unit_residential_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    bedrooms INTEGER,
    bathrooms INTEGER,
    furnished BOOLEAN NOT NULL DEFAULT FALSE,
    pet_policy VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uq_unit_residential_details_unit UNIQUE (unit_id, team_id),
    CONSTRAINT chk_unit_residential_bedrooms CHECK (bedrooms >= 0),
    CONSTRAINT chk_unit_residential_bathrooms CHECK (bathrooms >= 0)
);

-- =============================================================================
-- 3. unit_amenities
-- =============================================================================
CREATE TABLE unit_amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units (id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_unit_amenities UNIQUE (unit_id, amenity_id, team_id)
);

-- =============================================================================
-- 4. expense_allocations
-- =============================================================================
CREATE TABLE expense_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    expense_id UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES units (id),
    amount BIGINT NOT NULL,
    amount_currency VARCHAR(3) NOT NULL,
    basis VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_expense_allocations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_expense_allocations_basis CHECK (
        basis IN ('AREA', 'EQUAL', 'CUSTOM', 'MANUAL')
    )
);

CREATE UNIQUE INDEX uq_expense_allocations_expense_unit ON expense_allocations (expense_id, unit_id)
WHERE
    (deleted_at IS NULL);

CREATE INDEX idx_expense_allocations_unit_id ON expense_allocations (unit_id);

CREATE INDEX idx_expense_allocations_expense_id ON expense_allocations (expense_id);

-- =============================================================================
-- 5. properties.allocation_basis
-- =============================================================================
ALTER TABLE properties
ADD COLUMN allocation_basis VARCHAR(20) NOT NULL DEFAULT 'EQUAL';

ALTER TABLE properties
ADD CONSTRAINT chk_properties_allocation_basis CHECK (
    allocation_basis IN ('AREA', 'EQUAL', 'CUSTOM', 'MANUAL')
);

-- =============================================================================
-- 6. Backfill one implicit unit per property (soft-deleted ones included, so
--    their contracts keep a valid FK target).
-- =============================================================================
INSERT INTO
    units (
        identifier,
        team_id,
        property_id,
        unit_number,
        sort_order,
        unit_type,
        status,
        is_implicit,
        allocation_share,
        area_value,
        area_unit,
        energy_efficiency_rating,
        energy_certificate_expiry_date,
        heating_type,
        cooling_type,
        hot_water_system,
        insulation_notes,
        flooring_type,
        window_type,
        has_smoke_detectors,
        has_co_detectors,
        has_fire_extinguisher,
        has_adapted_bathroom,
        accessibility_notes,
        created_at,
        updated_at,
        created_by,
        updated_by,
        deleted_at
    )
SELECT
    'UNT' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.team_id,
    p.id,
    '1',
    0,
    CASE p.property_category
        WHEN 'COMMERCIAL' THEN 'COMMERCIAL'
        ELSE 'APARTMENT'
    END,
    p.status,
    TRUE,
    100,
    p.area_value,
    coalesce(p.area_unit, 'sqm'),
    p.energy_efficiency_rating,
    p.energy_certificate_expiry_date,
    p.heating_type,
    p.cooling_type,
    p.hot_water_system,
    p.insulation_notes,
    p.flooring_type,
    p.window_type,
    p.has_smoke_detectors,
    p.has_co_detectors,
    p.has_fire_extinguisher,
    p.has_adapted_bathroom,
    p.accessibility_notes,
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by,
    p.deleted_at
FROM
    properties p;

-- Copy residential details onto the implicit unit
INSERT INTO
    unit_residential_details (
        unit_id,
        team_id,
        bedrooms,
        bathrooms,
        furnished,
        pet_policy,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    u.id,
    d.team_id,
    d.bedrooms,
    d.bathrooms,
    d.furnished,
    d.pet_policy,
    d.created_at,
    d.updated_at,
    d.created_by,
    d.updated_by
FROM
    property_residential_details d
    JOIN units u ON u.property_id = d.property_id
    AND u.is_implicit;

-- Copy amenity links onto the implicit unit
INSERT INTO
    unit_amenities (
        unit_id,
        amenity_id,
        team_id,
        notes,
        created_at,
        updated_at,
        created_by,
        updated_by,
        deleted_at
    )
SELECT
    u.id,
    a.amenity_id,
    a.team_id,
    a.notes,
    a.created_at,
    a.updated_at,
    a.created_by,
    a.updated_by,
    a.deleted_at
FROM
    property_amenities a
    JOIN units u ON u.property_id = a.property_id
    AND u.is_implicit;

-- =============================================================================
-- 7. unit_id on contracts, occupancy periods and WWS calculations (NOT NULL)
-- =============================================================================
ALTER TABLE contracts
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE contracts c
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = c.property_id
    AND u.is_implicit;

ALTER TABLE contracts
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_contracts_unit_id ON contracts (unit_id);

ALTER TABLE property_occupancy_periods
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE property_occupancy_periods o
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = o.property_id
    AND u.is_implicit;

ALTER TABLE property_occupancy_periods
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_occupancy_periods_unit_id ON property_occupancy_periods (unit_id);

ALTER TABLE wws_calculations
ADD COLUMN unit_id UUID REFERENCES units (id);

UPDATE wws_calculations w
SET
    unit_id = u.id
FROM
    units u
WHERE
    u.property_id = w.property_id
    AND u.is_implicit;

ALTER TABLE wws_calculations
ALTER COLUMN unit_id
SET NOT NULL;

CREATE INDEX idx_wws_calculations_unit_id ON wws_calculations (unit_id);

-- =============================================================================
-- 8. Optional unit_id on photos, documents and expenses
-- =============================================================================
ALTER TABLE photos
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_photos_unit_id ON photos (unit_id);

ALTER TABLE documents
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_documents_unit_id ON documents (unit_id);

ALTER TABLE expenses
ADD COLUMN unit_id UUID REFERENCES units (id);

CREATE INDEX idx_expenses_unit_id ON expenses (unit_id);

-- =============================================================================
-- 9. Re-scope the occupancy no-overlap exclusion from property to unit
-- =============================================================================
ALTER TABLE property_occupancy_periods
DROP CONSTRAINT excl_occupancy_periods_no_overlap;

ALTER TABLE property_occupancy_periods
ADD CONSTRAINT excl_occupancy_periods_no_overlap EXCLUDE USING gist (
    unit_id
    WITH
        =,
        daterange (
            start_date,
            coalesce(end_date, '9999-12-31'::date),
            '[]'
        )
    WITH
        &&
)
WHERE
    (deleted_at IS NULL);

-- =============================================================================
-- 10. Drop what has moved. Property is now building-only.
-- =============================================================================
DROP TABLE property_amenities;

DROP TABLE property_residential_details;

ALTER TABLE properties
DROP COLUMN status,
DROP COLUMN area_value,
DROP COLUMN area_unit,
DROP COLUMN energy_efficiency_rating,
DROP COLUMN energy_certificate_expiry_date,
DROP COLUMN heating_type,
DROP COLUMN cooling_type,
DROP COLUMN hot_water_system,
DROP COLUMN insulation_notes,
DROP COLUMN flooring_type,
DROP COLUMN window_type,
DROP COLUMN has_smoke_detectors,
DROP COLUMN has_co_detectors,
DROP COLUMN has_fire_extinguisher,
DROP COLUMN has_adapted_bathroom,
DROP COLUMN accessibility_notes;
