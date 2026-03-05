-- Add token column for one-click email verification links
ALTER TABLE email_verification_codes
ADD COLUMN token VARCHAR(64);

CREATE UNIQUE INDEX idx_email_verification_codes_token ON email_verification_codes (token)
WHERE
    token IS NOT NULL;

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
