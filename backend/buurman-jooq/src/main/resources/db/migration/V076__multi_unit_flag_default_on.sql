-- Flip the multi_unit feature flag to enabled by default (BUUR-106 follow-up).
UPDATE feature_flags
SET
    default_enabled = TRUE,
    updated_at = now()
WHERE
    key = 'multi_unit';
