-- Bucket4j distributed rate limit state (PostgreSQL SELECT FOR UPDATE strategy)
-- Used by RateLimitFilter for cross-pod rate limiting
-- Not tenant-scoped: rate limiting is global per client IP
CREATE TABLE rate_limit_buckets (
    id VARCHAR(255) PRIMARY KEY,
    state BYTEA,
    expires_at BIGINT
);

CREATE INDEX idx_rate_limit_buckets_expires_at ON rate_limit_buckets (expires_at)
WHERE
    expires_at IS NOT NULL;

-- Generic rate limit configuration table
-- One row per rate-limited endpoint, managed via backoffice UI
CREATE TABLE rate_limit_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    max_requests INTEGER NOT NULL DEFAULT 10,
    period_seconds INTEGER NOT NULL DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255)
);

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
        'registration-validation',
        'Registration Code Validation',
        'Rate limit for POST /registration-invitations/validate per client IP',
        10,
        60
    );
