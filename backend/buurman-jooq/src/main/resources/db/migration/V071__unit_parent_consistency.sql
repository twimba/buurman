-- =============================================================================
-- V071__unit_parent_consistency.sql
-- Make the unit/property/team parent-consistency invariant unwritable at the
-- database level, instead of relying on it being enforced correctly at every
-- Java call site (BUUR-106 wave3c review).
-- =============================================================================
-- 1. contracts / property_occupancy_periods / wws_calculations: unit_id must
--    belong to the same property_id the row itself carries. expense_allocations
--    carries only unit_id (no property_id column), so it needs no such FK.
-- =============================================================================
ALTER TABLE units
ADD CONSTRAINT uq_units_id_property UNIQUE (id, property_id);

DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT count(*) INTO mismatch_count
    FROM contracts c
    JOIN units u ON u.id = c.unit_id
    WHERE u.property_id <> c.property_id;

    IF mismatch_count > 0 THEN
        RAISE EXCEPTION
            'contracts: % row(s) have unit_id pointing at a unit of a different property; fix before adding fk_contracts_unit_property',
            mismatch_count;
    END IF;
END $$;

ALTER TABLE contracts
ADD CONSTRAINT fk_contracts_unit_property FOREIGN KEY (unit_id, property_id) REFERENCES units (id, property_id);

DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT count(*) INTO mismatch_count
    FROM property_occupancy_periods o
    JOIN units u ON u.id = o.unit_id
    WHERE u.property_id <> o.property_id;

    IF mismatch_count > 0 THEN
        RAISE EXCEPTION
            'property_occupancy_periods: % row(s) have unit_id pointing at a unit of a different property; fix before adding fk_occupancy_periods_unit_property',
            mismatch_count;
    END IF;
END $$;

ALTER TABLE property_occupancy_periods
ADD CONSTRAINT fk_occupancy_periods_unit_property FOREIGN KEY (unit_id, property_id) REFERENCES units (id, property_id);

DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT count(*) INTO mismatch_count
    FROM wws_calculations w
    JOIN units u ON u.id = w.unit_id
    WHERE u.property_id <> w.property_id;

    IF mismatch_count > 0 THEN
        RAISE EXCEPTION
            'wws_calculations: % row(s) have unit_id pointing at a unit of a different property; fix before adding fk_wws_calculations_unit_property',
            mismatch_count;
    END IF;
END $$;

ALTER TABLE wws_calculations
ADD CONSTRAINT fk_wws_calculations_unit_property FOREIGN KEY (unit_id, property_id) REFERENCES units (id, property_id);

-- =============================================================================
-- 2. units.team_id must match its parent property's team_id -- today they are
--    independent columns with no composite FK, which is why
--    PropertyRepository.hasUnitWithStatus filters UNITS.TEAM_ID explicitly
--    "rather than relying solely on" the PROPERTIES join staying team-scoped.
-- =============================================================================
DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT count(*) INTO mismatch_count
    FROM units u
    JOIN properties p ON p.id = u.property_id
    WHERE p.team_id <> u.team_id;

    IF mismatch_count > 0 THEN
        RAISE EXCEPTION
            'units: % row(s) have team_id different from their property''s team_id; fix before adding fk_units_property_team',
            mismatch_count;
    END IF;
END $$;

ALTER TABLE properties
ADD CONSTRAINT uq_properties_id_team UNIQUE (id, team_id);

ALTER TABLE units
ADD CONSTRAINT fk_units_property_team FOREIGN KEY (property_id, team_id) REFERENCES properties (id, team_id);
