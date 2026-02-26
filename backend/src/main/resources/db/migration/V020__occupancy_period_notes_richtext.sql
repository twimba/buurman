-- Change notes column from VARCHAR(500) to TEXT to support rich text (HTML) content
ALTER TABLE property_occupancy_periods
ALTER COLUMN notes TYPE TEXT;
