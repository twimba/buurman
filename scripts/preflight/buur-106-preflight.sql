-- =============================================================================
-- BUUR-106 pre-flight checks — run against a RESTORED PRODUCTION SNAPSHOT
-- before deploying migrations V070..V072.
--
-- Read-only. Safe to run against production directly, though a snapshot is
-- preferred so that the rehearsal in buur-106-rehearsal.sh can follow it.
--
-- Every migration in this branch has only ever run against empty databases.
-- These checks assert the preconditions V070 and V072 silently assume. Each
-- ABORT below corresponds to a statement that will fail the deploy partway
-- through, leaving Flyway's schema_history marked failed and requiring the
-- snapshot restore the deploy plan already provides for.
--
-- Usage:
--   psql "$SNAPSHOT_URL" -v ON_ERROR_STOP=1 -f buur-106-preflight.sql
-- =============================================================================
\pset pager off
\timing on

\echo '==============================================================='
\echo 'BUUR-106 PRE-FLIGHT'
\echo '==============================================================='

-- Guard: this script is meaningless after the migration has already run.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'units') THEN
        RAISE EXCEPTION 'A "units" table already exists — V070 has already been applied to this database. Restore a pre-V070 snapshot.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = current_schema() AND table_name = 'properties') THEN
        RAISE EXCEPTION 'No "properties" table — this is not a Buurman database.';
    END IF;
END $$;

\echo ''
\echo '--- 0. Scale (drives the maintenance-window estimate) -----------'

SELECT 'properties' AS table_name, count(*) AS rows, pg_size_pretty(pg_total_relation_size('properties')) AS total_size FROM properties
UNION ALL SELECT 'contracts', count(*), pg_size_pretty(pg_total_relation_size('contracts')) FROM contracts
UNION ALL SELECT 'property_occupancy_periods', count(*), pg_size_pretty(pg_total_relation_size('property_occupancy_periods')) FROM property_occupancy_periods
UNION ALL SELECT 'wws_calculations', count(*), pg_size_pretty(pg_total_relation_size('wws_calculations')) FROM wws_calculations
UNION ALL SELECT 'property_residential_details', count(*), pg_size_pretty(pg_total_relation_size('property_residential_details')) FROM property_residential_details
UNION ALL SELECT 'property_amenities', count(*), pg_size_pretty(pg_total_relation_size('property_amenities')) FROM property_amenities
UNION ALL SELECT 'photos', count(*), pg_size_pretty(pg_total_relation_size('photos')) FROM photos
UNION ALL SELECT 'documents', count(*), pg_size_pretty(pg_total_relation_size('documents')) FROM documents
UNION ALL SELECT 'expenses', count(*), pg_size_pretty(pg_total_relation_size('expenses')) FROM expenses
ORDER BY 1;

\echo ''
\echo '--- 1. Sessions that would block the ALTER TABLEs ---------------'
\echo '    V070 takes ACCESS EXCLUSIVE on contracts, property_occupancy_periods'
\echo '    and wws_calculations. Anything here queues the migration, and every'
\echo '    later query queues behind the migration. Expect 0 rows at deploy'
\echo '    time (Quartz paused, app stopped).'

SELECT pid, state, wait_event_type, now() - xact_start AS xact_age, left(query, 80) AS query
FROM pg_stat_activity
WHERE pid <> pg_backend_pid()
  AND datname = current_database()
  AND xact_start IS NOT NULL
  AND now() - xact_start > interval '30 seconds'
ORDER BY xact_start;

SELECT count(*) AS prepared_transactions_must_be_zero FROM pg_prepared_xacts;

\echo ''
\echo '=== ABORT CONDITIONS ==========================================='
\echo 'Each row returned below is a row that fails the deploy.'

\echo ''
\echo '--- A1. properties.status NULL -> units.status is NOT NULL ------'
SELECT count(*) AS abort_properties_null_status FROM properties WHERE status IS NULL;

\echo ''
\echo '--- A2. child rows with no property -> unit_id stays NULL -------'
\echo '    V070 populates unit_id by joining units on property_id, then does'
\echo '    SET NOT NULL. A NULL or dangling property_id aborts that statement.'
SELECT 'contracts.property_id IS NULL' AS problem, count(*) AS abort_rows FROM contracts WHERE property_id IS NULL
UNION ALL SELECT 'contracts -> missing property', count(*) FROM contracts c WHERE c.property_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM properties p WHERE p.id = c.property_id)
UNION ALL SELECT 'occupancy_periods.property_id IS NULL', count(*) FROM property_occupancy_periods WHERE property_id IS NULL
UNION ALL SELECT 'occupancy_periods -> missing property', count(*) FROM property_occupancy_periods o WHERE o.property_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM properties p WHERE p.id = o.property_id)
UNION ALL SELECT 'wws_calculations.property_id IS NULL', count(*) FROM wws_calculations WHERE property_id IS NULL
UNION ALL SELECT 'wws_calculations -> missing property', count(*) FROM wws_calculations w WHERE w.property_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM properties p WHERE p.id = w.property_id)
ORDER BY 1;

\echo ''
\echo '--- A3. duplicate copy sources -> UNIQUE violation on the new tables'
\echo '    uq_unit_residential_details_unit (unit_id, team_id) and'
\echo '    uq_unit_amenities (unit_id, amenity_id, team_id): one implicit unit'
\echo '    per property, so duplicate source rows per property collide.'
SELECT 'property_residential_details duplicated per property' AS problem, count(*) AS abort_groups
FROM (SELECT property_id FROM property_residential_details GROUP BY property_id HAVING count(*) > 1) d
UNION ALL
SELECT 'property_amenities duplicated per (property, amenity)', count(*)
FROM (SELECT property_id, amenity_id FROM property_amenities GROUP BY property_id, amenity_id HAVING count(*) > 1) a
ORDER BY 1;

\echo ''
\echo '--- A4. >1 ACTIVE contract per property -------------------------'
\echo '    *** THE MOST LIKELY BLOCKER. READ THIS ONE CAREFULLY. ***'
\echo '    V070 gives every property exactly ONE implicit unit, so all of a'
\echo '    property''s contracts land on that single unit. V072 then creates'
\echo '    uq_contracts_one_active_per_unit and RAISES if any unit carries two'
\echo '    ACTIVE contracts. A duplex or HMO entered today as ONE property with'
\echo '    two active tenancies -- precisely the case BUUR-106 exists to fix --'
\echo '    aborts the deploy. Remediation options are in the runbook.'

SELECT count(*) AS abort_properties_with_multiple_active_contracts
FROM (
    SELECT property_id FROM contracts
    WHERE status = 'ACTIVE' AND deleted_at IS NULL
    GROUP BY property_id HAVING count(*) > 1
) x;

\echo '    ... and the offending properties, so they can be remediated:'
SELECT c.team_id,
       p.identifier AS property_identifier,
       p.property_category,
       count(*)     AS active_contracts,
       string_agg(c.identifier, ', ' ORDER BY c.identifier) AS contract_identifiers
FROM contracts c
JOIN properties p ON p.id = c.property_id
WHERE c.status = 'ACTIVE' AND c.deleted_at IS NULL
GROUP BY c.team_id, p.id, p.identifier, p.property_category
HAVING count(*) > 1
ORDER BY count(*) DESC, c.team_id
LIMIT 200;

\echo ''
\echo '--- A5. team_id divergence between a child row and its property -'
\echo '    V072 adds composite FKs. units.team_id is copied from the property,'
\echo '    so this only bites if a child row already disagrees with its parent.'
SELECT 'contracts vs property' AS problem, count(*) AS abort_rows
FROM contracts c JOIN properties p ON p.id = c.property_id WHERE c.team_id <> p.team_id
UNION ALL SELECT 'occupancy_periods vs property', count(*)
FROM property_occupancy_periods o JOIN properties p ON p.id = o.property_id WHERE o.team_id <> p.team_id
UNION ALL SELECT 'wws_calculations vs property', count(*)
FROM wws_calculations w JOIN properties p ON p.id = w.property_id WHERE w.team_id <> p.team_id
ORDER BY 1;

\echo ''
\echo '=== WARNINGS (deploy succeeds; data is silently altered) ========'

\echo ''
\echo '--- W1. property_category -> unit_type mapping ------------------'
\echo '    CASE maps RESIDENTIAL/MIXED_USE -> APARTMENT, everything else'
\echo '    (including NULL) -> COMMERCIAL. Check nothing residential is'
\echo '    hiding in the ELSE branch.'
SELECT coalesce(property_category, '(NULL)') AS property_category,
       count(*) AS properties,
       CASE property_category WHEN 'RESIDENTIAL' THEN 'APARTMENT'
                              WHEN 'MIXED_USE'   THEN 'APARTMENT'
                              ELSE 'COMMERCIAL' END AS becomes_unit_type
FROM properties
GROUP BY property_category
ORDER BY count(*) DESC;

\echo ''
\echo '--- W2. copy sources orphaned by the JOIN (silent data loss) ----'
\echo '    Both copy INSERTs JOIN units on property_id. A source row whose'
\echo '    property no longer exists is dropped without error, then its table'
\echo '    is DROPped seconds later -- unrecoverable except from the snapshot.'
SELECT 'property_residential_details with no property' AS problem, count(*) AS rows_silently_lost
FROM property_residential_details d WHERE NOT EXISTS (SELECT 1 FROM properties p WHERE p.id = d.property_id)
UNION ALL SELECT 'property_amenities with no property', count(*)
FROM property_amenities a WHERE NOT EXISTS (SELECT 1 FROM properties p WHERE p.id = a.property_id)
UNION ALL SELECT 'property_amenities with no amenity', count(*)
FROM property_amenities a WHERE NOT EXISTS (SELECT 1 FROM amenities m WHERE m.id = a.amenity_id)
ORDER BY 1;

\echo ''
\echo '--- W3. dwelling data on properties that will move to the unit --'
\echo '    Sanity counts to compare against the post-migration verification.'
SELECT count(*) AS properties_total,
       count(area_value) AS with_area_value,
       count(energy_efficiency_rating) AS with_energy_rating,
       count(*) FILTER (WHERE deleted_at IS NOT NULL) AS soft_deleted_also_backfilled
FROM properties;

\echo ''
\echo '--- W4. occupancy overlaps under the re-scoped GiST constraint --'
\echo '    V070 re-scopes excl_occupancy_periods_no_overlap from property_id to'
\echo '    unit_id. With one unit per property the two are equivalent, so this'
\echo '    should be 0; a non-zero value means the existing constraint is not'
\echo '    doing what its name says.'
SELECT count(*) AS overlapping_occupancy_pairs
FROM property_occupancy_periods a
JOIN property_occupancy_periods b
  ON a.property_id = b.property_id
 AND a.id < b.id
 AND a.deleted_at IS NULL AND b.deleted_at IS NULL
 AND daterange(a.start_date, a.end_date, '[]') && daterange(b.start_date, b.end_date, '[]');

\echo ''
\echo '==============================================================='
\echo 'PRE-FLIGHT COMPLETE.'
\echo 'GO only if every abort_* count above is 0.'
\echo 'Then run buur-106-rehearsal.sh for a timed, rolled-back rehearsal'
\echo 'of the real migrations against this same snapshot.'
\echo '==============================================================='
