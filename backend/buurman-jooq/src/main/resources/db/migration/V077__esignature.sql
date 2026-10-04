-- ===== from V077__esignature.sql =====
CREATE TABLE signature_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    document_id UUID NOT NULL REFERENCES documents (id),
    signed_document_id UUID REFERENCES documents (id),
    provider VARCHAR(32) NOT NULL,
    provider_submission_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_signature_requests_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_signature_requests_team ON signature_requests (team_id);

CREATE INDEX idx_signature_requests_document ON signature_requests (document_id);

CREATE UNIQUE INDEX idx_signature_requests_provider_submission ON signature_requests (provider, provider_submission_id);

CREATE TABLE signature_signers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signature_request_id UUID NOT NULL REFERENCES signature_requests (id),
    contact_id UUID REFERENCES contacts (id),
    email VARCHAR NOT NULL,
    role VARCHAR(32) NOT NULL,
    provider_signer_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    signed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_signature_signers_request ON signature_signers (signature_request_id);

-- ===== from V078__esignature_feature_flag.sql =====
INSERT INTO
    feature_flags (
        key,
        value_type,
        default_enabled,
        default_value,
        description
    )
VALUES
    (
        'esignature_enabled',
        'boolean',
        FALSE,
        NULL,
        'E-signature (Documenso) integration for generated letters/addenda'
    );

-- ===== from V088__signature_requests_document_team_index.sql =====
-- findByDocumentIdAndTeamId filters on (document_id, team_id) and sorts by created_at desc;
-- only single-column indexes existed. The new composite index covers the filter and the sort
-- in one pass, making the old document_id-only index redundant.
DROP INDEX idx_signature_requests_document;

CREATE INDEX idx_signature_requests_document_team ON signature_requests (document_id, team_id, created_at DESC);

-- ===== from V089__signature_signers_team_id.sql =====
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
