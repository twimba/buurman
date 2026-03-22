-- ---------------------------------------------------------------------------
-- V041: Backfill payments.contact_id from the contract's primary tenant party
-- ---------------------------------------------------------------------------
-- Auto-generated payments were created without contact_id. This migration
-- sets contact_id to the primary tenant of the linked contract for all
-- payments that currently have contact_id = NULL.
-- ---------------------------------------------------------------------------
UPDATE payments p
SET
    contact_id = cp.contact_id
FROM
    contract_parties cp
WHERE
    cp.contract_id = p.contract_id
    AND cp.team_id = p.team_id
    AND cp.role = 'PRIMARY_TENANT'
    AND cp.deleted_at IS NULL
    AND p.contact_id IS NULL
    AND p.deleted_at IS NULL;
