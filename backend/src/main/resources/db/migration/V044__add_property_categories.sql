-- Add property_category column
ALTER TABLE properties
ADD COLUMN property_category VARCHAR(50);

-- Backfill all existing properties as RESIDENTIAL
UPDATE properties
SET
    property_category = 'RESIDENTIAL';

-- Drop old property_type constraint BEFORE remapping (otherwise CHECK blocks new values)
ALTER TABLE properties
DROP CONSTRAINT IF EXISTS chk_properties_property_type;

-- Defensive: remap any COMMERCIAL type rows
UPDATE properties
SET
    property_category = 'COMMERCIAL',
    property_type = 'OTHER_COMMERCIAL'
WHERE
    property_type = 'COMMERCIAL';

-- Make NOT NULL after backfill
ALTER TABLE properties
ALTER COLUMN property_category
SET NOT NULL;

-- Add CHECK constraint for property_category
ALTER TABLE properties
ADD CONSTRAINT chk_properties_category CHECK (
    property_category IN (
        'RESIDENTIAL',
        'COMMERCIAL',
        'INDUSTRIAL',
        'AGRICULTURAL',
        'MIXED_USE'
    )
);

-- Add expanded property_type constraint
ALTER TABLE properties
ADD CONSTRAINT chk_properties_property_type CHECK (
    property_type IN (
        -- Residential
        'APARTMENT',
        'HOUSE',
        'STUDIO',
        'ROOM',
        'VILLA',
        'TOWNHOUSE',
        'OTHER_RESIDENTIAL',
        -- Commercial
        'OFFICE',
        'RETAIL',
        'RESTAURANT',
        'HOTEL',
        'SHOWROOM',
        'AUTO_DEALERSHIP',
        'SNACKBAR',
        'CAFE',
        'MOTEL',
        'BAR',
        'BED_AND_BREAKFAST',
        'OTHER_COMMERCIAL',
        -- Industrial
        'WAREHOUSE',
        'WORKSHOP',
        'FACTORY',
        'DATA_CENTER',
        'COLD_STORAGE',
        'GARAGE',
        'OTHER_INDUSTRIAL',
        -- Agricultural
        'FARMLAND',
        'RANCH',
        'GREENHOUSE',
        'ORCHARD',
        'VINEYARD',
        'OTHER_AGRICULTURAL',
        -- Mixed-Use
        'MIXED_USE',
        -- Legacy (keep COMMERCIAL for safety, mapped to OTHER_COMMERCIAL above)
        'COMMERCIAL'
    )
);

-- Drop old status constraint and replace with expanded one
ALTER TABLE properties
DROP CONSTRAINT IF EXISTS chk_properties_status;

ALTER TABLE properties
ADD CONSTRAINT chk_properties_status CHECK (
    status IN (
        'VACANT',
        'OCCUPIED',
        'MAINTENANCE',
        'UNAVAILABLE',
        'UNDER_RENOVATION',
        'FALLOW',
        'LISTED'
    )
);

-- Index on category
CREATE INDEX idx_properties_category ON properties (team_id, property_category)
WHERE
    deleted_at IS NULL;
