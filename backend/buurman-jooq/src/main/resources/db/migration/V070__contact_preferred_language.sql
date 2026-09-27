-- The language a contact should be written to in. Resolution order at send time is
-- contact preference, then the recipient user's preference, then the contract's document
-- language, then the team default, then English.
ALTER TABLE contacts
    ADD COLUMN preferred_language VARCHAR(2);

ALTER TABLE contacts
    ADD CONSTRAINT chk_contact_preferred_language
        CHECK (preferred_language IS NULL OR preferred_language IN
               ('en', 'nl', 'de', 'fr', 'pt', 'es', 'sv', 'it', 'fi', 'el', 'pl', 'da', 'nb'));
