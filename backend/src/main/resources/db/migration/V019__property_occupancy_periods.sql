-- Self-occupancy periods for properties (primary residence, vacation home, family use)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE property_occupancy_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    start_date DATE NOT NULL,
    end_date DATE,
    type VARCHAR(20) NOT NULL,
    occupant_name VARCHAR(255),
    monthly_imputed_rent NUMERIC(12, 2),
    end_reason VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_occupancy_periods_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_occupancy_periods_dates CHECK (
        end_date IS NULL
        OR end_date >= start_date
    ),
    CONSTRAINT chk_occupancy_periods_imputed_rent CHECK (
        monthly_imputed_rent IS NULL
        OR monthly_imputed_rent >= 0
    ),
    -- Prevent overlapping active periods on the same property
    CONSTRAINT excl_occupancy_periods_no_overlap EXCLUDE USING gist (
        property_id
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
        (deleted_at IS NULL)
);

CREATE INDEX idx_occupancy_periods_team_id ON property_occupancy_periods (team_id);

CREATE INDEX idx_occupancy_periods_property_id ON property_occupancy_periods (property_id);

CREATE INDEX idx_occupancy_periods_dates ON property_occupancy_periods (property_id, start_date, end_date);

CREATE INDEX idx_occupancy_periods_deleted_at ON property_occupancy_periods (deleted_at)
WHERE
    deleted_at IS NULL;
