-- Add latitude and longitude columns to tenant_addresses for map functionality
ALTER TABLE tenant_addresses
ADD COLUMN latitude DECIMAL(10, 8),
ADD COLUMN longitude DECIMAL(11, 8);

-- Add index for geospatial queries (optional, for future features like distance calculation)
CREATE INDEX idx_tenant_addresses_coordinates ON tenant_addresses(latitude, longitude)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
