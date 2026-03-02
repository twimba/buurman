-- V020: Add unit columns to commercial and industrial detail fields
-- that previously had hardcoded units (e.g., ceiling_height_m, clear_height_m)
-- =============================================
-- Commercial Details: ceiling_height_m -> ceiling_height_value + ceiling_height_unit
-- =============================================
ALTER TABLE property_commercial_details
RENAME COLUMN ceiling_height_m TO ceiling_height_value;

ALTER TABLE property_commercial_details
ADD COLUMN ceiling_height_unit VARCHAR(20);

ALTER TABLE property_commercial_details
DROP CONSTRAINT chk_commercial_ceiling_height;

ALTER TABLE property_commercial_details
ADD CONSTRAINT chk_commercial_ceiling_height CHECK (ceiling_height_value > 0);

-- Default existing data to meters
UPDATE property_commercial_details
SET
    ceiling_height_unit = 'm'
WHERE
    ceiling_height_value IS NOT NULL;

-- =============================================
-- Industrial Details: clear_height_m -> clear_height_value + clear_height_unit
-- =============================================
ALTER TABLE property_industrial_details
RENAME COLUMN clear_height_m TO clear_height_value;

ALTER TABLE property_industrial_details
ADD COLUMN clear_height_unit VARCHAR(20);

ALTER TABLE property_industrial_details
DROP CONSTRAINT chk_industrial_clear_height;

ALTER TABLE property_industrial_details
ADD CONSTRAINT chk_industrial_clear_height CHECK (clear_height_value > 0);

UPDATE property_industrial_details
SET
    clear_height_unit = 'm'
WHERE
    clear_height_value IS NOT NULL;

-- =============================================
-- Industrial Details: floor_load_capacity_kg_sqm -> floor_load_capacity_value + floor_load_capacity_unit
-- =============================================
ALTER TABLE property_industrial_details
RENAME COLUMN floor_load_capacity_kg_sqm TO floor_load_capacity_value;

ALTER TABLE property_industrial_details
ADD COLUMN floor_load_capacity_unit VARCHAR(20);

ALTER TABLE property_industrial_details
DROP CONSTRAINT chk_industrial_floor_load;

ALTER TABLE property_industrial_details
ADD CONSTRAINT chk_industrial_floor_load CHECK (floor_load_capacity_value > 0);

UPDATE property_industrial_details
SET
    floor_load_capacity_unit = 'kg_sqm'
WHERE
    floor_load_capacity_value IS NOT NULL;

-- =============================================
-- Industrial Details: crane_capacity_tons -> crane_capacity_value + crane_capacity_unit
-- =============================================
ALTER TABLE property_industrial_details
RENAME COLUMN crane_capacity_tons TO crane_capacity_value;

ALTER TABLE property_industrial_details
ADD COLUMN crane_capacity_unit VARCHAR(20);

ALTER TABLE property_industrial_details
DROP CONSTRAINT chk_industrial_crane;

ALTER TABLE property_industrial_details
ADD CONSTRAINT chk_industrial_crane CHECK (crane_capacity_value > 0);

UPDATE property_industrial_details
SET
    crane_capacity_unit = 'metric_tons'
WHERE
    crane_capacity_value IS NOT NULL;
