-- BUUR-96 follow-up: the platform should not force a default country on teams.
-- Country is user-chosen (onboarding / team settings); new teams start with no country
-- until the user sets one. Existing values are left untouched.
ALTER TABLE team_preferences
ALTER COLUMN default_country_code
DROP DEFAULT;
