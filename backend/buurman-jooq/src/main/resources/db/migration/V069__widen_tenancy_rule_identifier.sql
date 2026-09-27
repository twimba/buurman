-- Widen rent_regulation_tenancy_rules.identifier from VARCHAR(26) to VARCHAR(29).
--
-- Sids are generated as a 3-char EntityPrefix (RRT) + a 26-char ULID = 29 chars
-- (see SidGenerator). V068 created this column as VARCHAR(26), which overflows any Sid
-- produced by SidGenerator with "value too long for type character varying(26)" — the same
-- issue V058 already fixed for rent_regulation_countries/regions/rules.
ALTER TABLE rent_regulation_tenancy_rules
ALTER COLUMN identifier TYPE VARCHAR(29);
