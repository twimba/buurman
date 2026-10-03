-- findByDocumentIdAndTeamId filters on (document_id, team_id) and sorts by created_at desc;
-- only single-column indexes existed. The new composite index covers the filter and the sort
-- in one pass, making the old document_id-only index redundant.
DROP INDEX idx_signature_requests_document;

CREATE INDEX idx_signature_requests_document_team ON signature_requests (document_id, team_id, created_at DESC);
