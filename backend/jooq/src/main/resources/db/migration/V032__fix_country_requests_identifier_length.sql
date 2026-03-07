-- Fix identifier column length: 3-char prefix + 26-char ULID = 29 chars (matching all other tables)
ALTER TABLE rent_regulation_country_requests
ALTER COLUMN identifier TYPE VARCHAR(29);
