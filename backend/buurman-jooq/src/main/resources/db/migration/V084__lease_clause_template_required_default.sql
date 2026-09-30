-- A required (non-optional) clause template must default to included; otherwise a contract
-- with no override would silently produce a lease missing a clause the template says must
-- always be present. See LeaseClauseResolver, which additionally forces included=true at read
-- time for any non-optional template, regardless of this default or a stored override.
ALTER TABLE lease_clause_templates
ADD CONSTRAINT chk_lease_clause_templates_required_default CHECK (
    optional
    OR default_included
);
