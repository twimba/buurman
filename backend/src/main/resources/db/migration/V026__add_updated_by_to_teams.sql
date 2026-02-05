-- Add updated_by column to teams table for proper audit trail
ALTER TABLE teams ADD COLUMN updated_by UUID;
