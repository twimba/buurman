-- Seed the multi_unit feature flag (BUUR-106).
-- Disabled by default: several known defects (WWS pricing reads, occupancy reporting, expense
-- allocation edits) are still property-scoped rather than unit-scoped, and only manifest once a
-- property has more than one unit. Gating unit creation behind this flag keeps every property's
-- single, invisible implicit unit working for all teams while confining the multi-unit exposure
-- to pilot teams opted in via a per-team override.
INSERT INTO
    feature_flags (
        key,
        value_type,
        default_enabled,
        default_value,
        description
    )
VALUES
    (
        'multi_unit',
        'boolean',
        FALSE,
        NULL,
        'Allow creating more than one unit per property (pilot-only until WWS/occupancy/allocation are unit-scoped)'
    );
