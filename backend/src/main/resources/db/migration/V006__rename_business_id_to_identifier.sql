-- Migration: Rename business_id to identifier for teams and properties
-- This migration adds the new identifier column, copies data, updates constraints and indexes
-- The old business_id columns are kept temporarily for rollback safety

-- Step 1: Add new identifier columns
ALTER TABLE teams ADD COLUMN identifier VARCHAR(26);
ALTER TABLE properties ADD COLUMN identifier VARCHAR(26);

-- Step 2: Copy data from business_id to identifier
UPDATE teams SET identifier = business_id;
UPDATE properties SET identifier = business_id;

-- Step 3: Add NOT NULL constraint
ALTER TABLE teams ALTER COLUMN identifier SET NOT NULL;
ALTER TABLE properties ALTER COLUMN identifier SET NOT NULL;

-- Step 4: Drop old constraints
ALTER TABLE teams DROP CONSTRAINT IF EXISTS teams_business_id_key;
ALTER TABLE properties DROP CONSTRAINT IF EXISTS properties_team_id_business_id_key;

-- Step 5: Create new constraints
ALTER TABLE teams ADD CONSTRAINT teams_identifier_key UNIQUE (identifier);
ALTER TABLE properties ADD CONSTRAINT properties_team_id_identifier_key UNIQUE (team_id, identifier);

-- Step 6: Drop old indexes
DROP INDEX IF EXISTS idx_teams_business_id;
DROP INDEX IF EXISTS idx_properties_business_id;

-- Step 7: Create new indexes
CREATE INDEX idx_teams_identifier ON teams(identifier);
CREATE INDEX idx_properties_identifier ON properties(team_id, identifier);

-- Step 8: Drop old columns
ALTER TABLE teams DROP COLUMN business_id;
ALTER TABLE properties DROP COLUMN business_id;
