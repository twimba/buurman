-- Widen rent-regulation identifier columns from VARCHAR(26) to VARCHAR(29).
--
-- Sids are generated as a 3-char EntityPrefix (RRC/RRG/RRL) + a 26-char ULID = 29 chars
-- (see SidGenerator). V029 created these identifier columns as VARCHAR(26), which only fit the
-- hand-crafted seed identifiers and overflows any Sid produced by SidGenerator. This broke
-- backoffice create / bulk-import / catalog-reload inserts with
-- "value too long for type character varying(26)". rent_regulation_country_requests was already
-- VARCHAR(29); this aligns the remaining three tables.
ALTER TABLE rent_regulation_countries ALTER COLUMN identifier TYPE VARCHAR(29);

ALTER TABLE rent_regulation_regions ALTER COLUMN identifier TYPE VARCHAR(29);

ALTER TABLE rent_regulation_rules ALTER COLUMN identifier TYPE VARCHAR(29);
