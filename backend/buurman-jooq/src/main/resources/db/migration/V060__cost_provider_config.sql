-- BUUR-96: Admin-editable provider cost parameters, so figures that used to live only in
-- application.yml (Mailgun's flat plan fee + per-email rate) can be managed from the backoffice
-- Costs page without redeploying. Generic key/value so future provider knobs reuse the table.
-- Resolution at read time is DB-first, falling back to the application.yml seed when no row exists.
CREATE TABLE cost_config (
    config_key VARCHAR(60) PRIMARY KEY,
    value_text VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(200)
);
