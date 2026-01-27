CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    -- Address fields
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Netherlands',

    -- Specifications
    bedrooms INTEGER,
    bathrooms INTEGER,
    square_meters DECIMAL(10,2),
    property_type VARCHAR(50) NOT NULL CHECK (property_type IN ('APARTMENT', 'HOUSE', 'STUDIO', 'COMMERCIAL')),

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'VACANT' CHECK (status IN ('VACANT', 'OCCUPIED', 'MAINTENANCE', 'UNAVAILABLE')),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,

    -- Constraints
    UNIQUE(team_id, business_id),
    CHECK (bedrooms >= 0),
    CHECK (bathrooms >= 0),
    CHECK (square_meters > 0)
);

-- Indexes
CREATE INDEX idx_properties_team ON properties(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_status ON properties(team_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_business_id ON properties(team_id, business_id);
