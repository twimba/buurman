-- BUUR-93 §12 — CHECK constraints on dimensional columns added by V055.
-- Ensures invariants hold (build year ranges, EPC class ranges, contract date ordering).
ALTER TABLE rent_regulation_rules
ADD CONSTRAINT chk_rrr_build_year_range CHECK (
    build_year_min IS NULL
    OR build_year_max IS NULL
    OR build_year_min <= build_year_max
),
ADD CONSTRAINT chk_rrr_epc_class_range CHECK (
    epc_class_min IS NULL
    OR epc_class_min IN ('A', 'B', 'C', 'D', 'E', 'F', 'G')
),
ADD CONSTRAINT chk_rrr_epc_class_max CHECK (
    epc_class_max IS NULL
    OR epc_class_max IN ('A', 'B', 'C', 'D', 'E', 'F', 'G')
),
ADD CONSTRAINT chk_rrr_epc_class_order CHECK (
    epc_class_min IS NULL
    OR epc_class_max IS NULL
    OR epc_class_min <= epc_class_max
),
ADD CONSTRAINT chk_rrr_contract_date_order CHECK (
    contract_signed_after IS NULL
    OR contract_signed_before IS NULL
    OR contract_signed_after <= contract_signed_before
),
ADD CONSTRAINT chk_rrr_landlord_min_properties CHECK (
    landlord_min_properties IS NULL
    OR landlord_min_properties >= 1
),
ADD CONSTRAINT chk_rrr_build_year_min_sane CHECK (
    build_year_min IS NULL
    OR build_year_min BETWEEN 1800 AND 2100
),
ADD CONSTRAINT chk_rrr_build_year_max_sane CHECK (
    build_year_max IS NULL
    OR build_year_max BETWEEN 1800 AND 2100
);
