-- Add phone_verified_at to users table
ALTER TABLE users ADD COLUMN phone_verified_at TIMESTAMP;

-- Phone verification codes table (mirrors email_verification_codes)
CREATE TABLE phone_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pvc_user_id ON phone_verification_codes(user_id);
CREATE INDEX idx_pvc_user_code ON phone_verification_codes(user_id, code);
