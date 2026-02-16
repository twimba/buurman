-- Global amenities lookup table
CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    CONSTRAINT amenities_identifier_key UNIQUE (identifier),
    CONSTRAINT chk_amenities_category CHECK (
        category IN (
            'LEISURE',
            'COMFORT',
            'APPLIANCE',
            'STORAGE',
            'SERVICE'
        )
    )
);

-- Seed amenities
INSERT INTO
    amenities (identifier, name, category, icon)
VALUES
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Swimming Pool',
        'LEISURE',
        'swimming_pool'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Gym',
        'LEISURE',
        'gym'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Sauna',
        'LEISURE',
        'sauna'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Fireplace',
        'COMFORT',
        'fireplace'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Built-in Vacuum',
        'COMFORT',
        'built_in_vacuum'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'In-unit Laundry',
        'APPLIANCE',
        'in_unit_laundry'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Shared Laundry',
        'APPLIANCE',
        'shared_laundry'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Dishwasher',
        'APPLIANCE',
        'dishwasher'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Storage Unit',
        'STORAGE',
        'storage_unit'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Bike Storage',
        'STORAGE',
        'bike_storage'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Furnished',
        'COMFORT',
        'furnished'
    ),
    (
        'AMN' || upper(
            substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
        ),
        'Doorman / Concierge',
        'SERVICE',
        'doorman_concierge'
    );

-- Property-amenity join table (team-scoped)
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
