CREATE TABLE signature_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
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
