-- BUUR-93 §2 — drop the unused `sector` column.
-- `sector` was added in V029 alongside `property_category` but never populated in any
-- migration or codepath. The dimensional columns added in V055 make it permanently unused.
--
-- `property_category` is kept as-is for backwards compatibility this release; it will be
-- dropped in a future migration once all consumers (frontend, JOOQ codegen, API) are off it.
ALTER TABLE rent_regulation_rules
DROP COLUMN sector;

-- Add a comment marking property_category as deprecated (PostgreSQL column comment).
COMMENT ON COLUMN rent_regulation_rules.property_category IS 'DEPRECATED (BUUR-93): use the typed dimensional columns instead (regime, property_type, contract_type, tax_regime, tenancy_phase, build_year_min/max, epc_class_min/max, contract_signed_after/before, landlord_min_properties, area_code). Retained for backwards compat; to be dropped after consumer migration.';
