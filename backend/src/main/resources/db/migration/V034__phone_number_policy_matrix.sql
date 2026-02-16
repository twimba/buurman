-- Replace separate country/type columns with a single policy_matrix JSONB column.
-- The matrix maps country codes to their allowed number types:
-- {"NL": ["MOBILE", "FIXED_LINE_OR_MOBILE"], "US": ["MOBILE", "FIXED_LINE_OR_MOBILE"], ...}
-- Countries absent from the map are fully blocked.
ALTER TABLE phone_number_policy
ADD COLUMN policy_matrix JSONB NOT NULL DEFAULT '{}'::JSONB;

-- Seed default policy: EU (27) + North America (3) allowed for MOBILE and FIXED_LINE_OR_MOBILE
UPDATE phone_number_policy
SET
    policy_matrix = '{
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
}'::JSONB;

-- Drop old columns
ALTER TABLE phone_number_policy
DROP COLUMN country_mode;

ALTER TABLE phone_number_policy
DROP COLUMN allowed_countries;

ALTER TABLE phone_number_policy
DROP COLUMN blocked_countries;

ALTER TABLE phone_number_policy
DROP COLUMN allowed_number_types;
