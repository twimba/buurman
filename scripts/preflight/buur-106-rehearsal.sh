#!/usr/bin/env bash
# =============================================================================
# BUUR-106 migration rehearsal — runs the REAL V068/V069/V071 against a
# restored production snapshot, times each one, then ROLLS BACK.
#
# Postgres makes all the DDL in these three migrations transactional, so the
# whole sequence runs inside one BEGIN ... ROLLBACK. Nothing is committed: the
# snapshot is left byte-identical, and you still learn (a) whether the
# migrations succeed against real data and (b) how long the maintenance window
# has to be.
#
# This is the check that has never been performed. V068 and V071 have only ever
# run against empty databases, and V071 contains DO blocks that RAISE EXCEPTION
# on data that real tenants plausibly have (see abort condition A4 in
# buur-106-preflight.sql).
#
# Usage:
#   ./buur-106-rehearsal.sh "postgresql://user:pass@host:5432/snapshot_db"
#
# Run buur-106-preflight.sql FIRST. This script tells you whether the migration
# works; the pre-flight tells you why it doesn't.
# =============================================================================
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "usage: $0 <snapshot-database-url>" >&2
    exit 64
fi

SNAPSHOT_URL="$1"
MIGRATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../backend/buurman-jooq/src/main/resources/db/migration" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

MIGRATIONS=(V068__units.sql V069__multi_unit_flag.sql V071__unit_parent_consistency.sql)

for m in "${MIGRATIONS[@]}"; do
    if [[ ! -f "$MIGRATION_DIR/$m" ]]; then
        echo "FATAL: migration not found: $MIGRATION_DIR/$m" >&2
        exit 1
    fi
done

REHEARSAL="$WORK/rehearsal.sql"

{
    echo "\\set ON_ERROR_STOP on"
    echo "\\timing on"
    echo "\\pset pager off"
    echo "\\echo '=== REHEARSAL START (everything below is rolled back) ==='"
    echo "BEGIN;"
    # Fail fast rather than queueing behind a lock for the whole window.
    echo "SET LOCAL lock_timeout = '10s';"
    echo "SET LOCAL statement_timeout = '30min';"
    for m in "${MIGRATIONS[@]}"; do
        echo "\\echo ''"
        echo "\\echo '--- applying $m ---'"
        cat "$MIGRATION_DIR/$m"
        echo ""
    done

    # ---------------------------------------------------------------------
    # Post-migration verification, inside the same transaction. These assert
    # the migration actually moved the data, not merely that it did not error.
    # ---------------------------------------------------------------------
    cat <<'VERIFY'
\echo ''
\echo '--- POST-MIGRATION VERIFICATION (still inside the transaction) ---'

DO $$
DECLARE
    properties_count   INTEGER;
    implicit_count     INTEGER;
    bad_contracts      INTEGER;
    bad_occupancy      INTEGER;
    bad_wws            INTEGER;
    lost_area          INTEGER;
BEGIN
    SELECT count(*) INTO properties_count FROM properties;
    SELECT count(*) INTO implicit_count   FROM units WHERE is_implicit;
    IF properties_count <> implicit_count THEN
        RAISE EXCEPTION 'expected exactly one implicit unit per property: % properties but % implicit units',
            properties_count, implicit_count;
    END IF;

    -- Every child row must point at a unit of ITS OWN property, not just at
    -- some non-null unit. A backfill missing "u.property_id = c.property_id"
    -- would reattach every tenancy to an arbitrary building.
    SELECT count(*) INTO bad_contracts
      FROM contracts c JOIN units u ON u.id = c.unit_id WHERE u.property_id <> c.property_id;
    SELECT count(*) INTO bad_occupancy
      FROM property_occupancy_periods o JOIN units u ON u.id = o.unit_id WHERE u.property_id <> o.property_id;
    SELECT count(*) INTO bad_wws
      FROM wws_calculations w JOIN units u ON u.id = w.unit_id WHERE u.property_id <> w.property_id;
    IF bad_contracts + bad_occupancy + bad_wws > 0 THEN
        RAISE EXCEPTION 'cross-property unit attachment: % contracts, % occupancy periods, % wws rows',
            bad_contracts, bad_occupancy, bad_wws;
    END IF;

    -- The dwelling data must have actually landed on the units.
    SELECT count(*) INTO lost_area FROM units WHERE is_implicit AND area_value IS NULL;
    RAISE NOTICE 'implicit units without area_value (compare to pre-flight W3): %', lost_area;

    RAISE NOTICE 'VERIFICATION PASSED: % properties -> % implicit units, no cross-property attachment',
        properties_count, implicit_count;
END $$;

SELECT count(*) AS units_total,
       count(*) FILTER (WHERE is_implicit) AS implicit,
       count(*) FILTER (WHERE deleted_at IS NOT NULL) AS soft_deleted
FROM units;

SELECT unit_type, count(*) FROM units GROUP BY unit_type ORDER BY 2 DESC;

SELECT count(*) AS unit_residential_details_rows FROM unit_residential_details;
SELECT count(*) AS unit_amenities_rows FROM unit_amenities;

\echo ''
\echo '--- ROLLING BACK ---'
VERIFY
    echo "ROLLBACK;"
    echo "\\echo '=== REHEARSAL COMPLETE — snapshot unchanged ==='"
} > "$REHEARSAL"

echo "Rehearsing ${#MIGRATIONS[@]} migrations against the snapshot."
echo "Per-statement timings are printed by psql's \\timing; the total below is"
echo "your maintenance-window floor (add application restart time)."
echo

START=$(date +%s)
set +e
psql "$SNAPSHOT_URL" --no-psqlrc -f "$REHEARSAL"
PSQL_EXIT=$?
set -e
END=$(date +%s)

echo
echo "==============================================================="
if [[ $PSQL_EXIT -eq 0 ]]; then
    echo "REHEARSAL PASSED in $((END - START))s of wall clock."
    echo "The migration succeeds against this snapshot's real data."
    echo "Size the maintenance window from the timings above, not from this"
    echo "total — a rolled-back transaction skips some post-commit I/O."
else
    echo "REHEARSAL FAILED (psql exit $PSQL_EXIT) after $((END - START))s."
    echo "Do NOT deploy. The error above is the error production would hit."
    echo "Nothing was committed; the snapshot is unchanged."
fi
echo "==============================================================="
exit $PSQL_EXIT
