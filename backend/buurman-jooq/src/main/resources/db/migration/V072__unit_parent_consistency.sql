-- =============================================================================
-- V072__unit_parent_consistency.sql
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

-- =============================================================================
-- 3. At most one ACTIVE contract per unit. ContractRepository.findActiveByUnitId
--    used fetchOptional(), which throws TooManyRowsException (-> unhandled 500)
--    the moment two concurrent activations race past the application-level,
--    read-then-write guard. This turns the race into a 409 instead.
-- =============================================================================
DO $$
DECLARE
    dup_count INTEGER;
BEGIN
    SELECT count(*) INTO dup_count
    FROM (
        SELECT unit_id
        FROM contracts
        WHERE status = 'ACTIVE' AND deleted_at IS NULL
        GROUP BY unit_id
        HAVING count(*) > 1
    ) dups;

    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'contracts: % unit(s) already carry more than one ACTIVE contract; resolve before adding uq_contracts_one_active_per_unit',
            dup_count;
    END IF;
END $$;

CREATE UNIQUE INDEX uq_contracts_one_active_per_unit ON contracts (unit_id)
WHERE
    status = 'ACTIVE'
    AND deleted_at IS NULL;

-- =============================================================================
-- 4. Optimistic locking on units. UnitRepository.save()'s UPDATE branch writes
--    all columns from an in-memory snapshot with no version check and no
--    DELETED_AT IS NULL guard, so a concurrent promote + edit can resurrect
--    is_implicit after it was cleared (see UnitRepository for the write side).
-- =============================================================================
ALTER TABLE units
ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- =============================================================================
-- 5. Index hygiene.
-- =============================================================================
-- idx_units_property_id (property_id) is a redundant prefix of
-- uq_units_property_number (property_id, unit_number) WHERE deleted_at IS NULL,
-- which already serves every lookup the plain index did.
DROP INDEX idx_units_property_id;

-- The two hottest unit queries (findUnitCountsByTeamId, hasUnitWithStatus)
-- currently heap-fetch every unit in the team; this covers both.
CREATE INDEX idx_units_team_property_status ON units (team_id, property_id, status)
WHERE
    deleted_at IS NULL;

-- expense_allocations.team_id has ON DELETE CASCADE with no index, so a GDPR
-- erasure (DELETE FROM teams) sequential-scans an unbounded table --
-- replaceForExpense soft-deletes, so rows accumulate permanently.
CREATE INDEX idx_expense_allocations_team_id ON expense_allocations (team_id);

-- photos.unit_id, documents.unit_id and expenses.unit_id are ~100% NULL
-- (optional columns added in V070, populated only when the entity is
-- attached to a specific unit rather than the whole property). Make their
-- indexes partial so they don't carry dead weight for the common case.
DROP INDEX idx_photos_unit_id;

CREATE INDEX idx_photos_unit_id ON photos (unit_id)
WHERE
    unit_id IS NOT NULL;

DROP INDEX idx_documents_unit_id;

CREATE INDEX idx_documents_unit_id ON documents (unit_id)
WHERE
    unit_id IS NOT NULL;

DROP INDEX idx_expenses_unit_id;

CREATE INDEX idx_expenses_unit_id ON expenses (unit_id)
WHERE
    unit_id IS NOT NULL;
