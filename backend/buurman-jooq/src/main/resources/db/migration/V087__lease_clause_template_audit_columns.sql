-- lease_clause_templates is edited live by individual backoffice admins through ordinary CRUD
-- (unlike the rent_regulation_* reference tables, which are wholesale-replaced by a catalog
-- reload and deliberately carry no per-row audit trail), so who created/last touched a template
-- is real, useful information. The actor is a backoffice Keycloak subject id, not a row in the
-- app's own `users` table (a different realm), so these are plain UUID columns with no FK.
ALTER TABLE lease_clause_templates
ADD COLUMN created_by UUID,
ADD COLUMN updated_by UUID;

-- Existing rows predate this column and have no real actor to attribute to the backfill.
-- Matches Constants.SYSTEM_USER_ID, the same placeholder used for other system-attributed writes.
UPDATE lease_clause_templates
SET
    created_by = '00000000-0000-0000-0000-000000000001',
    updated_by = '00000000-0000-0000-0000-000000000001'
WHERE
    created_by IS NULL;

ALTER TABLE lease_clause_templates
ALTER COLUMN created_by
SET NOT NULL,
ALTER COLUMN updated_by
SET NOT NULL;
