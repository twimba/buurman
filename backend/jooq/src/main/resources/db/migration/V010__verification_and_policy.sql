-- =============================================================================
-- email_verification_codes
-- =============================================================================
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_evc_user_id ON email_verification_codes (user_id);

CREATE INDEX idx_evc_user_code ON email_verification_codes (user_id, code);

-- =============================================================================
-- phone_verification_codes
-- =============================================================================
CREATE TABLE phone_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pvc_user_id ON phone_verification_codes (user_id);

CREATE INDEX idx_pvc_user_code ON phone_verification_codes (user_id, code);

-- =============================================================================
-- phone_number_policy
-- =============================================================================
CREATE TABLE phone_number_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_matrix JSONB NOT NULL DEFAULT '{}'::JSONB,
    max_codes_per_hour INTEGER NOT NULL DEFAULT 3,
    verification_code_expiry_minutes INTEGER NOT NULL DEFAULT 10,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255)
);

-- Seed default policy: EU (27) + North America (3) allowed for MOBILE and FIXED_LINE_OR_MOBILE
INSERT INTO
    phone_number_policy (id, policy_matrix)
VALUES
    (
        gen_random_uuid(),
        '{
  "AT": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "BE": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "BG": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "HR": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "CY": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "CZ": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "DK": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "EE": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "FI": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "FR": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "DE": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "GR": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "HU": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "IE": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "IT": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "LV": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "LT": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "LU": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "MT": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "NL": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "PL": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "PT": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "RO": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "SK": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "SI": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "ES": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "SE": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "US": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "CA": ["MOBILE", "FIXED_LINE_OR_MOBILE"],
  "MX": ["MOBILE", "FIXED_LINE_OR_MOBILE"]
}'::JSONB
    );
