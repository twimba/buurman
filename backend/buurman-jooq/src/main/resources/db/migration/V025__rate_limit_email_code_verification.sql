INSERT INTO
    rate_limit_config (
        key,
        display_name,
        description,
        max_requests,
        period_seconds
    )
VALUES
    (
        'email-code-verification',
        'Email Code Verification',
        'Rate limit for POST /auth/verify-email per client IP',
        10,
        60
    )
ON CONFLICT (key) DO NOTHING;
