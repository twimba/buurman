# BUUR-106 deploy pre-flight and rehearsal

Migrations `V068__units.sql`, `V069__multi_unit_flag.sql` and
`V071__unit_parent_consistency.sql` have only ever run against **empty** databases (fresh
Testcontainers). This directory is what closes that gap before they meet production data.

`V068` is **irreversible**: it drops 16 columns from `properties` and drops the
`property_amenities` and `property_residential_details` tables, seconds after copying them onto the
new implicit units. There is no rollback migration. Recovery is the snapshot.

## Deploy plan this assumes

Agreed for this release:

- **Announced maintenance window.** V068 is not split into expand/contract, so the old application
  version cannot serve reads once it commits.
- **Quartz paused before the deploy**, so no scheduled job holds a lock the `ALTER TABLE`s must
  queue behind.
- **Snapshot taken immediately before, stop-the-world.** That snapshot is the rollback plan, which
  is why no forward-recovery migration was written.

## Verified, not just written

Both scripts were exercised end to end against a throwaway `postgres:18-alpine` with migrations
V001-V067 applied (a genuine pre-V068 schema), seeded with two properties and a duplex holding two
ACTIVE contracts:

- pre-flight ran clean (exit 0) and abort condition **A4 reported 1**, naming the offending property
  and both contract identifiers;
- the rehearsal applied V068 and V069, then **V071 raised**
  `contracts: 1 unit(s) already carry more than one ACTIVE contract`, and the script exited non-zero
  with "Do NOT deploy" -- i.e. it catches this before a maintenance window, not during one;
- after re-statusing the surplus contract, the rehearsal **passed**, its verification block reported
  `2 properties -> 2 implicit units, no cross-property attachment`, and rolled back;
- after the rollback the database was byte-equivalent: no `units` table, `property_amenities` still
  present, `properties.area_value` still present.

So the multiple-active-contract abort is a demonstrated failure mode, not a theoretical one.

## Order of operations

1. Restore the production snapshot somewhere private.
2. `psql "$SNAPSHOT_URL" -v ON_ERROR_STOP=1 -f buur-106-preflight.sql`
   Read-only. **GO only if every `abort_*` count is 0.**
3. `./buur-106-rehearsal.sh "$SNAPSHOT_URL"`
   Needs a postgres client. If `psql` is not on the host, set `PSQL_CMD`, e.g.
   `PSQL_CMD="docker run --rm -i --network host postgres:18-alpine psql" ./buur-106-rehearsal.sh "$SNAPSHOT_URL"`.
   Runs the real migrations on the real data inside one transaction, verifies the result, then rolls
   back. Snapshot is left unchanged. This is what sizes the maintenance window.
4. Deploy for real, during the window, with Quartz paused and the fresh snapshot in hand.

Steps 2 and 3 are cheap. Step 3 is the one that actually proves the deploy works.

## The abort condition most likely to fire

**A4 — a property with more than one ACTIVE contract.**

V068 gives every existing property exactly **one** implicit unit, so all of that property's
contracts attach to that one unit. V071 then creates
`uq_contracts_one_active_per_unit` and raises if any unit carries two active contracts.

So a duplex, an HMO, or a subdivided house that a landlord entered **today as a single property with
two active tenancies** aborts the deploy. That is not an edge case — it is precisely the situation
BUUR-106 was built to fix, which makes it over-represented among exactly the customers who most want
this feature.

If A4 is non-zero, pick one before deploying:

- **(a) Pre-split the properties by hand** in the old schema — one property per tenancy. Correct
  data, but it changes what the landlord sees and loses the building grouping.
- **(b) End or re-status the surplus contracts.** Fastest, and wrong: these are live tenancies, and
  the contract status is legally meaningful.
- **(c) Write a data-repair migration (recommended).** A `V070`-style step that runs *between* V068
  and V071: for each property with N active contracts, create N−1 additional **non-implicit** units
  and move the surplus contracts onto them. This is the only option that preserves both the tenancy
  records and the building grouping, and it leaves the customer with exactly the multi-unit building
  the feature is about. It needs the `unit_number` naming decision made deliberately, since tenants
  will see it on letters.

Option (c) is not written yet. Run the pre-flight against a snapshot first: if A4 comes back 0, none
of this is needed, and if it comes back non-zero the row list tells you how much work (c) is.

## What the rehearsal verifies beyond "no error"

- exactly one implicit unit per property, soft-deleted properties included;
- every `contracts` / `property_occupancy_periods` / `wws_calculations` row points at a unit of
  **its own** property — catching a backfill that reattached tenancies to arbitrary buildings, which
  a `NOT NULL` check alone would not;
- the dwelling data actually landed on the units, with counts to compare against pre-flight W3.
