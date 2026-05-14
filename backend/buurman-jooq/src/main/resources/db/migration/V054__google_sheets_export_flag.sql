-- Seed the google_sheets_export feature flag (BUUR-94).
-- Disabled by default; operators flip the flag per team via the backoffice UI. The website
-- advertises Google Sheets export as part of the Big and Mega plans, but plan-driven enablement
-- is intentionally deferred to a follow-up — flag enablement remains manual for v1.
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
        'google_sheets_export',
        'boolean',
        FALSE,
        NULL,
        'Export to Google Sheets (Drive scope: drive.file)'
    );
