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
        'email-token-verification',
        'Email Token Verification',
        'Rate limit for GET /auth/verify-email-token per client IP',
        10,
        60
    );
