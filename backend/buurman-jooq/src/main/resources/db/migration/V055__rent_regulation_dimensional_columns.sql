-- BUUR-93 §3 — schema evolution for multi-dimensional rent-regulation regimes.
-- Adds typed dimensional columns so that the single overloaded `property_category` slot
-- can be replaced by an axis-per-column representation. NULL = wildcard "applies regardless
-- of this axis". Existing rows are populated in V056.
ALTER TABLE rent_regulation_rules
-- Regulatory regime (was overloaded into property_category): REGULATED, FREE_SECTOR,
-- MIDDLE_RENT, CEILING_RENT, KAPPUNGSGRENZE, MIETPREISBREMSE, ZONA_TENSIONADA,
-- COST_BASED, RICHTWERT, SMÅHUSE, FREE_MARKET, etc.
ADD COLUMN regime VARCHAR(40),
-- Building / occupancy type: RESIDENTIAL, COMMERCIAL, STUDENT, SOCIAL, MIXED, SEASONAL.
ADD COLUMN property_type VARCHAR(40),
-- Contract form: CANONE_LIBERO, CANONE_CONCORDATO, INSTITUTIONAL, OCCASIONAL,
-- STANDARD_AST, ASSURED_PERIODIC, etc.
ADD COLUMN contract_type VARCHAR(40),
-- Tax regime: CEDOLARE_SECCA, ORDINARY, IRS_DEDUCTION, etc.
ADD COLUMN tax_regime VARCHAR(40),
-- Phase of tenancy: EXISTING_LEASE, NEW_LEASE, RENEWAL, RELET, MODERNISATION.
ADD COLUMN tenancy_phase VARCHAR(20),
-- Build-year window: e.g. DK pre/post 1992, AT pre/post 1953.
ADD COLUMN build_year_min INT,
ADD COLUMN build_year_max INT,
-- EPC class window: e.g. BE EPC-tiered freezes, FR DPE F/G ban.
ADD COLUMN epc_class_min CHAR(1),
ADD COLUMN epc_class_max CHAR(1),
-- Contract signature date window: e.g. ES pre/post Ley 12/2023 (2023-05-26 threshold).
ADD COLUMN contract_signed_after DATE,
ADD COLUMN contract_signed_before DATE,
-- Landlord-size threshold: e.g. ES "gran tenedor" (5+ properties in zona tensionada).
ADD COLUMN landlord_min_properties INT,
-- Region-internal area code (e.g. FR encadrement zone, MD county sub-area).
ADD COLUMN area_code VARCHAR(40);

-- Index supporting the specificity-ranked rule-resolution query.
CREATE INDEX idx_rent_reg_rules_dimensions ON rent_regulation_rules (
    country_id,
    YEAR,
    regime,
    property_type,
    contract_type
);
