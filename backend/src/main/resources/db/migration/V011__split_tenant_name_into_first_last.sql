-- Split tenant name into first_name and last_name
ALTER TABLE tenants ADD COLUMN first_name VARCHAR(255);
ALTER TABLE tenants ADD COLUMN last_name VARCHAR(255);

-- Migrate existing name data
-- Try to split on first space, otherwise put everything in first_name
UPDATE tenants
SET
    first_name = CASE
        WHEN POSITION(' ' IN name) > 0
        THEN SUBSTRING(name FROM 1 FOR POSITION(' ' IN name) - 1)
        ELSE name
    END,
    last_name = CASE
        WHEN POSITION(' ' IN name) > 0
        THEN SUBSTRING(name FROM POSITION(' ' IN name) + 1)
        ELSE NULL
    END
WHERE name IS NOT NULL;

-- Make first_name NOT NULL after migration
ALTER TABLE tenants ALTER COLUMN first_name SET NOT NULL;

-- Drop old name column
ALTER TABLE tenants DROP COLUMN name;

-- Add comments
COMMENT ON COLUMN tenants.first_name IS 'Tenant first name';
COMMENT ON COLUMN tenants.last_name IS 'Tenant last name';
