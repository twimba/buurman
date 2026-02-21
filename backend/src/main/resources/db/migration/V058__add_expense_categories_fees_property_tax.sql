-- Drop all enum-style CHECK constraints; application enums handle validation
-- expenses
ALTER TABLE expenses
DROP CONSTRAINT chk_expenses_category;

-- contracts
ALTER TABLE contracts
DROP CONSTRAINT chk_contracts_contract_type;

ALTER TABLE contracts
DROP CONSTRAINT chk_contracts_payment_frequency;

ALTER TABLE contracts
DROP CONSTRAINT chk_contracts_status;

-- contract_parties
ALTER TABLE contract_parties
DROP CONSTRAINT chk_contract_parties_role;

-- contract_payment_instructions
ALTER TABLE contract_payment_instructions
DROP CONSTRAINT chk_cpi_custom_method;

-- payments
ALTER TABLE payments
DROP CONSTRAINT chk_payments_status;

-- properties
ALTER TABLE properties
DROP CONSTRAINT chk_properties_property_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_status;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_category;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_area_unit;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_construction_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_foundation_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_roof_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_flooring_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_window_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_heating_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_cooling_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_hot_water_system;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_sewage_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_internet_connection_type;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_internet_status;

ALTER TABLE properties
DROP CONSTRAINT chk_properties_parking_type;

-- property_outdoor_areas
ALTER TABLE property_outdoor_areas
DROP CONSTRAINT chk_outdoor_areas_type;

ALTER TABLE property_outdoor_areas
DROP CONSTRAINT chk_outdoor_areas_area_unit;

-- property_residential_details
ALTER TABLE property_residential_details
DROP CONSTRAINT chk_residential_pet_policy;

-- property_commercial_details
ALTER TABLE property_commercial_details
DROP CONSTRAINT chk_commercial_usable_area_unit;

ALTER TABLE property_commercial_details
DROP CONSTRAINT chk_commercial_common_area_unit;

-- property_industrial_details
ALTER TABLE property_industrial_details
DROP CONSTRAINT chk_industrial_yard_area_unit;

-- property_agricultural_details
ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_land_area_unit;

ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_arable_area_unit;

ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_soil_type;

ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_water_source;

ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_irrigation;

ALTER TABLE property_agricultural_details
DROP CONSTRAINT chk_agricultural_fencing;

-- amenities
ALTER TABLE amenities
DROP CONSTRAINT chk_amenities_category;

-- documents
ALTER TABLE documents
DROP CONSTRAINT chk_documents_entity_type;

-- photos
ALTER TABLE photos
DROP CONSTRAINT chk_photos_entity_type;

-- tenant_addresses
ALTER TABLE tenant_addresses
DROP CONSTRAINT chk_tenant_addresses_type;

ALTER TABLE tenant_addresses
DROP CONSTRAINT chk_tenant_addresses_status;

-- payment_instructions
ALTER TABLE payment_instructions
DROP CONSTRAINT chk_pi_payment_method;

-- calendar_feeds
ALTER TABLE calendar_feeds
DROP CONSTRAINT chk_calendar_feeds_feed_type;

-- generated_reports
ALTER TABLE generated_reports
DROP CONSTRAINT chk_reports_type;

ALTER TABLE generated_reports
DROP CONSTRAINT chk_reports_format;

ALTER TABLE generated_reports
DROP CONSTRAINT chk_reports_status;

-- user_preferences
ALTER TABLE user_preferences
DROP CONSTRAINT chk_user_preferences_theme;

-- user_notification_type_preferences
ALTER TABLE user_notification_type_preferences
DROP CONSTRAINT chk_notif_type;

-- notifications
ALTER TABLE notifications
DROP CONSTRAINT chk_notifications_channel;

ALTER TABLE notifications
DROP CONSTRAINT chk_notifications_status;

-- notification_outbox
ALTER TABLE notification_outbox
DROP CONSTRAINT chk_outbox_status;

-- audit_log
ALTER TABLE audit_log
DROP CONSTRAINT chk_audit_log_action;

-- job_execution_history
ALTER TABLE job_execution_history
DROP CONSTRAINT chk_exec_status;

-- team_members
ALTER TABLE team_members
DROP CONSTRAINT chk_team_members_role;

-- team_invitations
ALTER TABLE team_invitations
DROP CONSTRAINT chk_team_invitations_role;

-- team_preferences
ALTER TABLE team_preferences
DROP CONSTRAINT chk_fiscal_month;
