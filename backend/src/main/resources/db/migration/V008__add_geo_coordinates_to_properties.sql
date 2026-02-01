-- Add latitude and longitude columns to properties table
ALTER TABLE properties
    ADD COLUMN latitude DECIMAL(10, 8),
    ADD COLUMN longitude DECIMAL(11, 8);

-- Add index for geo queries (if needed in future)
CREATE INDEX idx_properties_coordinates ON properties(latitude, longitude) WHERE deleted_at IS NULL;

-- Add comment
COMMENT ON COLUMN properties.latitude IS 'Latitude coordinate from geocoding';
COMMENT ON COLUMN properties.longitude IS 'Longitude coordinate from geocoding';
