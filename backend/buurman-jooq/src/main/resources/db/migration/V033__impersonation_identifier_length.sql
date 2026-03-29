-- Fix identifier column length: 3-char prefix + 26-char ULID = 29 chars
ALTER TABLE impersonation_sessions
ALTER COLUMN identifier TYPE VARCHAR(29);
