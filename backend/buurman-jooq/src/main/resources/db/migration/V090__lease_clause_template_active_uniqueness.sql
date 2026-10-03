-- uq_lease_clause_templates_country_key_version allows two different versions of the same
-- (country_code, clause_key) to both be active (deleted_at IS NULL) at once — nothing enforces
-- supersede-on-save. Not exploited today, but a partial unique index closes the gap so a future
-- bug (e.g. a new version inserted without soft-deleting the old one) fails loudly at the
-- database instead of silently resolving two active templates for the same clause.
CREATE UNIQUE INDEX uq_lease_clause_templates_active_country_key ON lease_clause_templates (country_code, clause_key)
WHERE
    deleted_at IS NULL;
