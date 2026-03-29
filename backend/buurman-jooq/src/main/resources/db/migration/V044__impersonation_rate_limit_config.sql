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
        'impersonation-exchange',
        'Impersonation Token Exchange',
        'Rate limit for POST /auth/impersonate/exchange per client IP',
        5,
        60
    )
ON CONFLICT (key) DO NOTHING;
