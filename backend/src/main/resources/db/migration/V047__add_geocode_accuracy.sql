ALTER TABLE properties
ADD COLUMN geocode_accuracy VARCHAR(30);

ALTER TABLE tenant_addresses
ADD COLUMN geocode_accuracy VARCHAR(30);
