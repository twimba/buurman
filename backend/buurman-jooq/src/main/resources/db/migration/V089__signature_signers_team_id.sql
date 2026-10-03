-- signature_signers had no team_id of its own, relying entirely on its signature_requests join
-- for tenant isolation. No live leak today (every caller already scopes through the request),
-- but every other table in the schema carries team_id directly for defense-in-depth — add it
-- here too, backfilled from the parent request.
ALTER TABLE signature_signers
ADD COLUMN team_id UUID REFERENCES teams (id);

UPDATE signature_signers ss
SET
    team_id = sr.team_id
FROM
    signature_requests sr
WHERE
    sr.id = ss.signature_request_id;

ALTER TABLE signature_signers
ALTER COLUMN team_id
SET NOT NULL;

DROP INDEX idx_signature_signers_request;

CREATE INDEX idx_signature_signers_request_team ON signature_signers (signature_request_id, team_id);
