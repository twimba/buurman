ALTER TABLE users
ADD COLUMN disabled_at TIMESTAMP;

CREATE INDEX idx_users_disabled_at ON users (disabled_at);
