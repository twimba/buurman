-- BUUR-20: Add IP address tracking to impersonation sessions
ALTER TABLE impersonation_sessions
ADD COLUMN ip_address VARCHAR(45);
