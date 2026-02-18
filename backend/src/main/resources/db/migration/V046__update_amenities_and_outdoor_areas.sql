-- Add applicable_categories to amenities
ALTER TABLE amenities
ADD COLUMN applicable_categories VARCHAR(50) [];

-- Update existing amenities with applicable categories
UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Swimming Pool';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Gym';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Sauna';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Fireplace';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL}'
WHERE
    name = 'Built-in Vacuum';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL}'
WHERE
    name = 'In-unit Laundry';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Shared Laundry';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL}'
WHERE
    name = 'Dishwasher';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL,INDUSTRIAL}'
WHERE
    name = 'Storage Unit';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Bike Storage';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL}'
WHERE
    name = 'Furnished';

UPDATE amenities
SET
    applicable_categories = '{RESIDENTIAL,COMMERCIAL}'
WHERE
    name = 'Doorman / Concierge';

-- Insert new category-specific amenities
INSERT INTO
    amenities (
        identifier,
        name,
        category,
        icon,
        applicable_categories
    )
VALUES
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

-- Update outdoor areas type constraint to include new types
ALTER TABLE property_outdoor_areas
DROP CONSTRAINT chk_outdoor_areas_type;

ALTER TABLE property_outdoor_areas
ADD CONSTRAINT chk_outdoor_areas_type CHECK (
    type IN (
        -- Residential
        'BALCONY',
        'TERRACE',
        'GARDEN',
        'ROOFTOP',
        'PATIO',
        'YARD',
        'OTHER',
        -- Commercial / Industrial
        'PARKING_LOT',
        'COURTYARD',
        'LOADING_AREA',
        'STORAGE_AREA',
        -- Agricultural
        'PASTURE',
        'PADDOCK',
        'ORCHARD_AREA',
        'GREENHOUSE_AREA'
    )
);
