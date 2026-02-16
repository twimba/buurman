-- Make tenant email optional
ALTER TABLE tenants
ALTER COLUMN email
DROP NOT NULL;

-- Replace unique index to allow multiple NULL emails per team
DROP INDEX IF EXISTS idx_tenants_team_email_unique;

CREATE UNIQUE INDEX idx_tenants_team_email_unique ON tenants (team_id, email)
WHERE
    email IS NOT NULL
    AND deleted_at IS NULL;
