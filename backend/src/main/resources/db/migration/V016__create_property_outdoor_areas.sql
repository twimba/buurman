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
    CONSTRAINT chk_outdoor_areas_type CHECK (
        type IN (
            'BALCONY',
            'TERRACE',
            'GARDEN',
            'ROOFTOP',
            'PATIO',
            'YARD',
            'OTHER'
        )
    ),
    CONSTRAINT chk_outdoor_areas_area_value CHECK (area_value > 0),
    CONSTRAINT chk_outdoor_areas_area_unit CHECK (area_unit IN ('sqm', 'sqft'))
);

CREATE INDEX idx_property_outdoor_areas_team ON property_outdoor_areas (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_property_outdoor_areas_team_property ON property_outdoor_areas (team_id, property_id)
WHERE
    deleted_at IS NULL;
