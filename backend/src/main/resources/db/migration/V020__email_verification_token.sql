-- Add token column for one-click email verification links
ALTER TABLE email_verification_codes ADD COLUMN token VARCHAR(64);

CREATE UNIQUE INDEX idx_email_verification_codes_token
    ON email_verification_codes (token)
    WHERE token IS NOT NULL;
