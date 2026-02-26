-- =============================================================================
-- V016__financing_payment_balance_tracking.sql
-- Add balance_deducted flag to financing_payments to track whether a payment's
-- principal was deducted from the financing balance.
-- =============================================================================
ALTER TABLE financing_payments
ADD COLUMN balance_deducted BOOLEAN NOT NULL DEFAULT FALSE;
