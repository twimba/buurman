-- Add email_verified_at to users (existing users grandfathered as verified)
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;
UPDATE users SET email_verified_at = created_at WHERE email_verified_at IS NULL;

-- Verification codes table
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evc_user_id ON email_verification_codes(user_id);
CREATE INDEX idx_evc_user_code ON email_verification_codes(user_id, code);
