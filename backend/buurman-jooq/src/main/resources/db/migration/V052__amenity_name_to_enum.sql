-- Migrate amenity names from free-text English to enum values
UPDATE amenities
SET
    name = 'SWIMMING_POOL'
WHERE
    name = 'Swimming Pool';

UPDATE amenities
SET
    name = 'GYM'
WHERE
    name = 'Gym';

UPDATE amenities
SET
    name = 'SAUNA'
WHERE
    name = 'Sauna';

UPDATE amenities
SET
    name = 'FIREPLACE'
WHERE
    name = 'Fireplace';

UPDATE amenities
SET
    name = 'BUILT_IN_VACUUM'
WHERE
    name = 'Built-in Vacuum';

UPDATE amenities
SET
    name = 'IN_UNIT_LAUNDRY'
WHERE
    name = 'In-unit Laundry';

UPDATE amenities
SET
    name = 'SHARED_LAUNDRY'
WHERE
    name = 'Shared Laundry';

UPDATE amenities
SET
    name = 'DISHWASHER'
WHERE
    name = 'Dishwasher';

UPDATE amenities
SET
    name = 'STORAGE_UNIT'
WHERE
    name = 'Storage Unit';

UPDATE amenities
SET
    name = 'BIKE_STORAGE'
WHERE
    name = 'Bike Storage';

UPDATE amenities
SET
    name = 'FURNISHED'
WHERE
    name = 'Furnished';

UPDATE amenities
SET
    name = 'DOORMAN_CONCIERGE'
WHERE
    name = 'Doorman / Concierge';

UPDATE amenities
SET
    name = 'CONFERENCE_ROOM'
WHERE
    name = 'Conference Room';

UPDATE amenities
SET
    name = 'RECEPTION_AREA'
WHERE
    name = 'Reception Area';

UPDATE amenities
SET
    name = 'SERVER_ROOM'
WHERE
    name = 'Server Room';

UPDATE amenities
SET
    name = 'KITCHENETTE'
WHERE
    name = 'Kitchenette';

UPDATE amenities
SET
    name = 'LOADING_BAY'
WHERE
    name = 'Loading Bay';

UPDATE amenities
SET
    name = 'COMPRESSED_AIR_SYSTEM'
WHERE
    name = 'Compressed Air System';

UPDATE amenities
SET
    name = 'CHEMICAL_STORAGE'
WHERE
    name = 'Chemical Storage';

UPDATE amenities
SET
    name = 'OVERHEAD_CRANE'
WHERE
    name = 'Overhead Crane';

UPDATE amenities
SET
    name = 'IRRIGATION_SYSTEM'
WHERE
    name = 'Irrigation System';

UPDATE amenities
SET
    name = 'GRAIN_SILO'
WHERE
    name = 'Grain Silo';

UPDATE amenities
SET
    name = 'LIVESTOCK_SHELTER'
WHERE
    name = 'Livestock Shelter';

UPDATE amenities
SET
    name = 'COLD_STORAGE'
WHERE
    name = 'Cold Storage';

UPDATE amenities
SET
    name = 'SECURITY_BOOTH'
WHERE
    name = 'Security Booth';

-- Add CHECK constraint to enforce enum values
ALTER TABLE amenities
ADD CONSTRAINT chk_amenity_name CHECK (
    name IN (
        'SWIMMING_POOL',
        'GYM',
        'SAUNA',
        'FIREPLACE',
        'BUILT_IN_VACUUM',
        'IN_UNIT_LAUNDRY',
        'SHARED_LAUNDRY',
        'DISHWASHER',
        'STORAGE_UNIT',
        'BIKE_STORAGE',
        'FURNISHED',
        'DOORMAN_CONCIERGE',
        'CONFERENCE_ROOM',
        'RECEPTION_AREA',
        'SERVER_ROOM',
        'KITCHENETTE',
        'LOADING_BAY',
        'COMPRESSED_AIR_SYSTEM',
        'CHEMICAL_STORAGE',
        'OVERHEAD_CRANE',
        'IRRIGATION_SYSTEM',
        'GRAIN_SILO',
        'LIVESTOCK_SHELTER',
        'COLD_STORAGE',
        'SECURITY_BOOTH'
    )
);
