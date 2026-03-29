-- V035__drop_deprecated_renewal_columns.sql
-- Drop deprecated auto_renewal and renewal_notice_days columns (BUUR-78 cleanup)
-- These have been replaced by renewal_mode + landlord_notice_days + tenant_notice_days in V034
ALTER TABLE contracts
DROP COLUMN IF EXISTS auto_renewal;

ALTER TABLE contracts
DROP COLUMN IF EXISTS renewal_notice_days;
