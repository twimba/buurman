-- Formal-notice deadline (days the tenant gets to settle after a formal demand letter).
-- Country default lives in the regulation catalogue; a contract may override it.
ALTER TABLE rent_regulation_countries
    ADD COLUMN formal_notice_days INTEGER,
    ADD CONSTRAINT chk_rent_regulation_formal_notice_days
        CHECK (formal_notice_days IS NULL OR (formal_notice_days >= 1 AND formal_notice_days <= 365));

ALTER TABLE contracts
    ADD COLUMN formal_notice_days INTEGER,
    ADD CONSTRAINT chk_contracts_formal_notice_days
        CHECK (formal_notice_days IS NULL OR (formal_notice_days >= 1 AND formal_notice_days <= 365));
