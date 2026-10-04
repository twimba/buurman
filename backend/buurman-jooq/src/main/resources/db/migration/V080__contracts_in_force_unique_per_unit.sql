-- ===== from V085__contracts_in_force_unique_per_unit.sql =====
-- =============================================================================
-- V085__contracts_in_force_unique_per_unit.sql
-- Widen the DB-level one-contract-per-unit guarantee to cover NOTICE_GIVEN, not
-- just ACTIVE.
--
-- A prior fix (ContractService#assertUnitHasNoActiveContract /
-- ContractRepository#findActiveByUnitId) established that a NOTICE_GIVEN
-- contract still occupies its unit: the tenant is still living there until the
-- termination's effective end date, and only Contract.ContractStatus.IN_FORCE
-- ({ACTIVE, NOTICE_GIVEN}) should ever be treated as "this unit is taken".
-- uq_contracts_one_active_per_unit (V072), however, only ever indexed
-- status = 'ACTIVE', so the application-level guard was the sole backstop for
-- NOTICE_GIVEN -- a race could still let a unit end up with an ACTIVE and a
-- NOTICE_GIVEN contract (or two NOTICE_GIVEN ones) at once.
--
-- Postgres cannot ALTER a partial index's predicate in place, so this drops
-- the old, narrower index and creates its replacement under a new name that
-- reflects what it actually guards now.
-- =============================================================================
-- 1. Pre-flight: confirm no unit already carries more than one IN_FORCE
--    contract. The application's state machine only ever reaches NOTICE_GIVEN
--    FROM ACTIVE on the SAME contract row (ContractService#transitionStatus:
--    ACTIVE -> NOTICE_GIVEN -> TERMINATED is the only path in; nothing creates
--    a new ACTIVE/NOTICE_GIVEN contract on a unit that already has one), so
--    this is expected to find nothing -- but verify rather than assume.
-- =============================================================================
DO $$
DECLARE
    dup_count INTEGER;
BEGIN
    SELECT count(*) INTO dup_count
    FROM (
        SELECT unit_id
        FROM contracts
        WHERE status IN ('ACTIVE', 'NOTICE_GIVEN') AND deleted_at IS NULL
        GROUP BY unit_id
        HAVING count(*) > 1
    ) dups;

    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'contracts: % unit(s) already carry more than one in-force (ACTIVE or NOTICE_GIVEN) contract; resolve before adding uq_contracts_one_in_force_per_unit',
            dup_count;
    END IF;
END $$;

-- =============================================================================
-- 2. Replace the index.
-- =============================================================================
DROP INDEX uq_contracts_one_active_per_unit;

CREATE UNIQUE INDEX uq_contracts_one_in_force_per_unit ON contracts (unit_id)
WHERE
    status IN ('ACTIVE', 'NOTICE_GIVEN')
    AND deleted_at IS NULL;
