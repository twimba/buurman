-- Expert validation corrections (review panels: NL/DE/BE, PT/ES/FR, IT/AT/CH/LU, IE/GB,
-- Nordics, US/CA, PL/CZ). Applies critical historical data corrections, fills missing
-- regulatory frameworks (BE-Wallonia, GB-Wales, GB-NI, LU 5%-of-capital, PL Art. 8a,
-- CZ §2249, FI ARA, DE Mietpreisbremse §556d), and corrects materially wrong values.
--
-- Out of scope (tracked separately): comprehensive German Länder rollout (NRW/HE/BW/BB/NI/RP/SH),
-- US states (MA/MN/ME/NJ/DC/MD), Canadian provinces (MB/NB/NS/PE/NL), Swiss cantons (GE/VD),
-- France encadrement des loyers cities (~69 zones).

-- ============================================================
-- Netherlands (NL) — fix 2025 regulated value, free-sector effective dates, backfill middenhuur
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 5.00,
    notes = 'Social housing cap: 5.00% for rents ≥ €350 (€25 absolute uplift for rents < €350). Lower-rent freeze proposal withdrawn 2025-06-03.',
    source_url = 'https://www.huurcommissie.nl/actueel/nieuws/2025/03/13/huurverhoging-per-1-juli-2025',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000007';

UPDATE rent_regulation_rules
SET effective_date = '2023-01-01',
    notes = 'Wet maximering huurprijsverhogingen: free-sector cap applied calendar-year basis (anniversary indexing in practice).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000004';

UPDATE rent_regulation_rules
SET effective_date = '2024-01-01',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000006';

UPDATE rent_regulation_rules
SET effective_date = '2025-01-01',
    notes = 'CPI 3.1% + 1% = 4.1%. Wet maximering extended through 2027-05-01.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000008';

UPDATE rent_regulation_rules
SET effective_date = '2026-01-01',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000010';

-- Backfill historical middenhuur tier (Wet betaalbare huur in force 2024-07-01)
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000142',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'NL'),
    2024, 'MIDDLE_RENT', 5.50, 'CPI_LINKED', 'CPI + 1% (transitional)',
    '2024-07-01', 'ANNUAL',
    'https://www.huurcommissie.nl/onderwerpen/wet-betaalbare-huur',
    'Transitional: Wet betaalbare huur entered into force 2024-07-01. First-year cap aligned with free sector.'
), (
    'RRL01J00000000000000000143',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'NL'),
    2025, 'MIDDLE_RENT', 7.70, 'FIXED_PERCENTAGE', 'CAO loonontwikkeling +1pp',
    '2025-01-01', 'ANNUAL',
    'https://www.rijksoverheid.nl/actueel/nieuws/2024/12/17/maximale-huurverhoging-vanaf-1-januari-2025-41-procent-voor-vrije-sector-en-77-procent-voor-middenhuur',
    'Middenhuur tier (Wet betaalbare huur): 7.70% confirmed 2024-12-17.'
);

-- ============================================================
-- Germany (DE) — fix Kappungsgrenze frequency (triennial, not annual); add Mietpreisbremse §556d
-- ============================================================
UPDATE rent_regulation_rules
SET frequency = 'TRIENNIAL',
    max_increase_type = 'STATUTORY_CAP',
    index_name = 'Mietspiegel',
    notes = 'Kappungsgrenze §558 BGB: rent uplift to comparable local rent (Mietspiegel) capped at 20% over rolling 36 months. Tight-market designated areas: 15% / 36 months.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'DE')
  AND identifier IN (
    'RRL01J00000000000000000011','RRL01J00000000000000000012','RRL01J00000000000000000013',
    'RRL01J00000000000000000014','RRL01J00000000000000000015',
    'RRL01J00000000000000000016','RRL01J00000000000000000017','RRL01J00000000000000000018',
    'RRL01J00000000000000000019','RRL01J00000000000000000020'
  );

-- Mietpreisbremse §556d BGB (initial rent on re-letting, capped at Mietspiegel + 10%)
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES
('RRL01J00000000000000000150', (SELECT id FROM rent_regulation_countries WHERE country_code='DE'),
 2022, 'NEW_LEASE', 10.00, 'CAP_OVER_INDEX', 'Mietspiegel + 10%',
 '2022-01-01', 'ON_RELET',
 'https://www.gesetze-im-internet.de/bgb/__556d.html',
 'Mietpreisbremse §556d BGB: initial rent in designated tight markets capped at Mietspiegel + 10%.'),
('RRL01J00000000000000000151', (SELECT id FROM rent_regulation_countries WHERE country_code='DE'),
 2023, 'NEW_LEASE', 10.00, 'CAP_OVER_INDEX', 'Mietspiegel + 10%',
 '2023-01-01', 'ON_RELET',
 'https://www.gesetze-im-internet.de/bgb/__556d.html',
 'Mietpreisbremse Verordnungen reissued in multiple Länder.'),
('RRL01J00000000000000000152', (SELECT id FROM rent_regulation_countries WHERE country_code='DE'),
 2024, 'NEW_LEASE', 10.00, 'CAP_OVER_INDEX', 'Mietspiegel + 10%',
 '2024-01-01', 'ON_RELET',
 'https://www.gesetze-im-internet.de/bgb/__556d.html',
 'Mietpreisbremse continues. New-build exemption (§556f BGB) and modernisation exemption apply.'),
('RRL01J00000000000000000153', (SELECT id FROM rent_regulation_countries WHERE country_code='DE'),
 2025, 'NEW_LEASE', 10.00, 'CAP_OVER_INDEX', 'Mietspiegel + 10%',
 '2025-01-01', 'ON_RELET',
 'https://www.gesetze-im-internet.de/bgb/__556d.html',
 'Mietpreisbremse Verordnungen extended in NRW/HE/BW/SH/RP/NI/BB.'),
('RRL01J00000000000000000154', (SELECT id FROM rent_regulation_countries WHERE country_code='DE'),
 2026, 'NEW_LEASE', 10.00, 'CAP_OVER_INDEX', 'Mietspiegel + 10%',
 '2026-01-01', 'ON_RELET',
 'https://www.bundestag.de/dokumente/textarchiv/2025/kw26-de-mietpreisbremse-1084786',
 'Mietpreisbremse §556d BGB extended to 2029-12-31 by Bundestag KW26/2025.');

-- Modernisierungsumlage §559 BGB (modernisation cost pass-through)
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, additional_conditions, source_url, notes
) VALUES (
    'RRL01J00000000000000000160',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'DE'),
    2026, 'MODERNISATION', 8.00, 'COST_PASS_THROUGH', '§559 BGB',
    '2026-01-01', 'PER_MODERNISATION',
    'Absolute Kappungsgrenze: €3/m²/6y (non-tight markets); €2/m²/6y (tight markets).',
    'https://www.gesetze-im-internet.de/bgb/__559.html',
    'Modernisierungsumlage: 8% p.a. of modernisation cost may be added to rent.'
);

-- ============================================================
-- Portugal (PT) — fix 2025 coefficient (was 6.01%, actual 2.16%) and 2024 misleading note
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 2.16,
    index_value = 1.0216,
    notes = 'Aviso n.º 19772/2024 (INE, 2024-09-11). Coefficient 1.0216.',
    source_url = 'https://www.portaldahabitacao.pt/coeficientes-de-atualizacao-de-rendas',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'PT')
  AND YEAR = 2025
  AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET notes = 'INE coefficient 1.0694 (+6.94%). No cap applied. State provided IRS deduction to landlords whose 2023 updates stayed below CPI.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'PT')
  AND YEAR = 2024
  AND property_category = 'ALL';

-- ============================================================
-- France (FR) — fix 2024/2025 IRL values; add DPE F/G rent freeze and DOM/Corse IRL variants
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.50,
    index_name = 'IRL T1 2024',
    index_value = 143.46,
    notes = 'IRL T1 2024 = 143.46, +3.50% YoY (INSEE). Bouclier loyer expired Q1 2024.',
    source_url = 'https://www.insee.fr/fr/statistiques/serie/001515333',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'FR')
  AND YEAR = 2024
  AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET max_increase_percentage = 1.40,
    index_name = 'IRL T1 2025',
    index_value = 145.47,
    notes = 'IRL T1 2025 = 145.47, +1.40% YoY (INSEE).',
    source_url = 'https://www.insee.fr/fr/statistiques/8558868',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'FR')
  AND YEAR = 2025
  AND property_category = 'ALL';

-- DPE F/G rent freeze (loi Climat & Résilience, effective 2022-08-24)
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000200',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'FR'),
    2026, 'DPE_F_OR_G', 0.00, 'FIXED_PERCENTAGE', 'Gel des loyers (loi Climat)',
    '2026-01-01', 'ANNUAL',
    'https://www.ecologie.gouv.fr/politiques-publiques/location-gel-loyers-passoires-energetiques',
    'Since 2022-08-24, dwellings rated DPE F/G are barred from IRL revision (gel des loyers). G also barred from new leases since 2025-01-01.'
),
('RRL01J00000000000000000201',
 (SELECT id FROM rent_regulation_countries WHERE country_code='FR'),
 2026, 'DOM', 0.78, 'INDEX_LINKED', 'IRL T1 2026 (DOM)',
 '2026-04-01', 'QUARTERLY',
 'https://www.insee.fr/fr/statistiques/8974207',
 'Article 73 Constitution: IRL DOM T1 2026 = 143.78.'),
('RRL01J00000000000000000202',
 (SELECT id FROM rent_regulation_countries WHERE country_code='FR'),
 2026, 'CORSE', 0.78, 'INDEX_LINKED', 'IRL T1 2026 (Corse)',
 '2026-04-01', 'QUARTERLY',
 'https://www.insee.fr/fr/statistiques/8974207',
 'Corsica IRL T1 2026 = 142.38.');

UPDATE rent_regulation_rules
SET index_value = 143.78
WHERE identifier = 'RRL01J00000000000000000201';
UPDATE rent_regulation_rules
SET index_value = 142.38
WHERE identifier = 'RRL01J00000000000000000202';

-- ============================================================
-- Spain (ES) — fix 2022 mechanism, V053 note, add post-2023-05-26 category & zonas tensionadas
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_type = 'INDEX_LINKED',
    index_name = 'IGC capped at 2%',
    notes = 'RDL 6/2022 (2022-03-29): annual revision = min(IGC variation, 2%). IGC published monthly by INE.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'ES')
  AND YEAR = 2022
  AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET notes = 'IRAV index (monthly, INE) mandatory for primary-residence contracts signed on/after 2023-05-26. May 2026 IRAV ≈ 2.47% (March IRAV applied). Pre-2023-05-26 contracts may still use IPC unless updated. RDL 8/2026 obligatory 2-year extension ended 2026-04-29.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'ES')
  AND YEAR = 2026
  AND property_category = 'ALL';

INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES
('RRL01J00000000000000000210',
 (SELECT id FROM rent_regulation_countries WHERE country_code='ES'),
 2026, 'CONTRACT_POST_2023_05_26', 2.47, 'INDEX_LINKED', 'IRAV (Mar 2026)',
 '2026-05-01', 'MONTHLY',
 'https://www.ine.es/jaxiT3/Tabla.htm?t=72975',
 'Mandatory IRAV (art. 18 LAU mod. Ley 12/2023) for primary-residence contracts signed on/after 2023-05-26.'),
('RRL01J00000000000000000211',
 (SELECT id FROM rent_regulation_countries WHERE country_code='ES'),
 2026, 'ZONA_TENSIONADA_GRAN_TENEDOR', NULL, 'INDEX_LINKED', 'SERPAVI / Índice de Referencia',
 '2026-01-01', 'ANNUAL',
 'https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-5214',
 'Large landlords (5+ properties in declared zona tensionada) must cap new-contract rents at SERPAVI reference. 271 Catalan municipalities declared 2024. Non-large landlords: new rent ≤ previous rent + IRAV.'),
('RRL01J00000000000000000212',
 (SELECT id FROM rent_regulation_countries WHERE country_code='ES'),
 2026, 'SEASONAL', NULL, 'FREE_MARKET', NULL,
 '2026-01-01', 'ANNUAL',
 'https://www.boe.es/buscar/act.php?id=BOE-A-1994-26003',
 'Alquileres de temporada / habitación: outside LAU vivienda habitual; not subject to IRAV. Catalonia attempted regulation via Decret-Llei 6/2024.');

-- ============================================================
-- Belgium (BE) — fix fabricated historical YoY values; add Wallonia rules; EPC-tiered conditions
-- ============================================================
-- Flanders 2022-2025 corrections (gezondheidsindex annual averages per Statbel)
UPDATE rent_regulation_rules
SET max_increase_percentage = 9.30,
    additional_conditions = 'From 2022-10-01 to 2023-09-30: EPC A/B/C = full index; EPC D = 50% of index; EPC E/F/no-EPC = 0% (freeze).',
    notes = 'Gezondheidsindex YoY annual average 2022 ≈ 9.3% (Statbel). EPC-tiered freeze active from 2022-10-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000036';

UPDATE rent_regulation_rules
SET max_increase_percentage = 4.30,
    additional_conditions = 'EPC-tiered freeze active until 2023-09-30 (VLG decree 2022-09-30).',
    notes = 'Gezondheidsindex YoY annual average 2023 ≈ 4.3% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000037';

UPDATE rent_regulation_rules
SET max_increase_percentage = 3.30,
    notes = 'Gezondheidsindex YoY 2024 = 3.28% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000038';

UPDATE rent_regulation_rules
SET max_increase_percentage = 2.60,
    notes = 'Gezondheidsindex YoY 2025 = 2.63% (Statbel). Rebased 2025=100 from 2026-01-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000039';

-- Brussels 2022-2025 corrections
UPDATE rent_regulation_rules
SET max_increase_percentage = 9.30,
    notes = 'Gezondheidsindex YoY annual average 2022 ≈ 9.3% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000041';

UPDATE rent_regulation_rules
SET effective_date = '2022-10-14',
    additional_conditions = 'EPC A/B/C/D: full indexation. EPC E: 50% of index. EPC F/G: 0% (no indexation). In force 2022-10-14 → 2023-10-13.',
    notes = 'Brussels Ordinance 14-10-2022. EPC-tiered freeze. Single percentage shown is the cap for non-restricted properties.',
    source_url = 'https://be.brussels/en/housing/rental/lease-contracts/rental-price-indexation',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000042';

UPDATE rent_regulation_rules
SET max_increase_percentage = 3.30,
    notes = 'Gezondheidsindex YoY 2024 = 3.28% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000043';

UPDATE rent_regulation_rules
SET max_increase_percentage = 2.60,
    notes = 'Gezondheidsindex YoY 2025 = 2.63% (Statbel). Rebased 2025=100 from 2026-01-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000044';

-- Wallonia rules 2022-2026 (previously empty)
INSERT INTO rent_regulation_rules (
    identifier, country_id, region_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, additional_conditions, source_url, notes
) VALUES
('RRL01J00000000000000000170',
 (SELECT id FROM rent_regulation_countries WHERE country_code='BE'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WAL'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='BE')),
 2022, 'ALL', 9.30, 'INDEX_LINKED', 'Gezondheidsindex',
 '2022-01-01', 'ANNUAL',
 'From 2022-11-01: EPC A/B/C = full index; EPC D = 75% of index; EPC E = 50%; EPC F/G/no-PEB = 0% (freeze).',
 'https://wallex.wallonie.be/',
 'Walloon Decree 19-10-2022. Gezondheidsindex YoY 2022 ≈ 9.3%.'),
('RRL01J00000000000000000171',
 (SELECT id FROM rent_regulation_countries WHERE country_code='BE'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WAL'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='BE')),
 2023, 'ALL', 4.30, 'INDEX_LINKED', 'Gezondheidsindex',
 '2023-01-01', 'ANNUAL',
 'EPC-tiered freeze active until 2023-10-31. After 2023-11-01: PEB restrictions lifted.',
 'https://wallex.wallonie.be/',
 'Gezondheidsindex YoY 2023 ≈ 4.3% (Statbel).'),
('RRL01J00000000000000000172',
 (SELECT id FROM rent_regulation_countries WHERE country_code='BE'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WAL'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='BE')),
 2024, 'ALL', 3.30, 'INDEX_LINKED', 'Gezondheidsindex',
 '2024-01-01', 'ANNUAL', NULL,
 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
 'Gezondheidsindex YoY 2024 = 3.28% (Statbel).'),
('RRL01J00000000000000000173',
 (SELECT id FROM rent_regulation_countries WHERE country_code='BE'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WAL'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='BE')),
 2025, 'ALL', 2.60, 'INDEX_LINKED', 'Gezondheidsindex',
 '2025-01-01', 'ANNUAL', NULL,
 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
 'Gezondheidsindex YoY 2025 = 2.63% (Statbel).'),
('RRL01J00000000000000000174',
 (SELECT id FROM rent_regulation_countries WHERE country_code='BE'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WAL'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='BE')),
 2026, 'ALL', 3.20, 'INDEX_LINKED', 'Gezondheidsindex (rebased 2025=100)',
 '2026-01-01', 'ANNUAL', NULL,
 'https://www.plan.be/en/data/consumer-price-index-inflation-forecasts',
 'Federal Planning Bureau forecast 3.2% for 2026.');

-- ============================================================
-- Ireland (IE) — fix 2022 effective date and mechanism (RPZ HICP-linked, not fixed 2%)
-- ============================================================
UPDATE rent_regulation_rules
SET effective_date = '2021-12-11',
    max_increase_type = 'INDEX_LINKED',
    index_name = 'lesser of HICP or 2%',
    notes = 'RPZ: lesser of HICP or 2% per annum (Residential Tenancies (Amendment) Act 2021, in force 11-Dec-2021). Pre-11-Dec-2021 cap was 4%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000046';

-- ============================================================
-- Italy (IT) — fix all 2022-2025 ISTAT FOI values; add canone libero (FREE_MARKET); cedolare secca
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.60,
    notes = '75% of ISTAT FOI Jan-2022 YoY ~4.8% = 3.60%. Applied at contract anniversary, on written request.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000051';

UPDATE rent_regulation_rules
SET max_increase_percentage = 7.95,
    notes = '75% of ISTAT FOI Jan-2023 YoY ~10.6% = 7.95%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000052';

UPDATE rent_regulation_rules
SET max_increase_percentage = 0.60,
    notes = '75% of ISTAT FOI Jan-2024 YoY ~0.8% = 0.60%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000053';

UPDATE rent_regulation_rules
SET max_increase_percentage = 1.05,
    notes = '75% of ISTAT FOI Jan-2025 YoY ~1.4% = 1.05%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000054';

UPDATE rent_regulation_rules
SET additional_conditions = 'Cedolare secca regime (D.Lgs. 23/2011 art. 3 c.11) forbids any ISTAT adjustment for the contract''s duration.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'IT');

-- Canone libero (free market 4+4) — 100% FOI permitted if contract specifies
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000250',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'IT'),
    2026, 'FREE_MARKET', 1.50, 'INDEX_LINKED', 'ISTAT FOI (100%)',
    '2026-01-01', 'ANNUAL',
    'https://www.confedilizia.it/locazioni/indice-istat/',
    'Canone libero (4+4 contracts): up to 100% of FOI permitted when contractually stipulated.'
);

-- ============================================================
-- Austria (AT) — fix 2024 (suspended) and 2025 (5.00% capped, not 4.50%); add free-market category
-- ============================================================
UPDATE rent_regulation_rules
SET notes = 'Richtwert valorisation 1-Apr-2023 = +8.6% (statutory; no deferment).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000057';

UPDATE rent_regulation_rules
SET max_increase_percentage = 0.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    effective_date = '2024-04-01',
    notes = '3. MILG: Richtwert- und Kategorievalorisierung für 2024 ausgesetzt (no statutory adjustment).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000058';

UPDATE rent_regulation_rules
SET max_increase_percentage = 5.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    notes = '1-Apr-2025 valorisation capped at 5% by 3./4. MILG (uncapped VPI Ø 2024 / Ø 2023 ≈ 7.8%).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000059';

INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_type, effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000240',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'AT'),
    2026, 'FREE_MARKET', 'MARKET_RENT',
    '2026-01-01', 'ANNUAL',
    'https://www.ris.bka.gv.at/GeltendeFassung.wxe?Abfrage=Bundesnormen&Gesetzesnummer=10002531',
    'Vollausnahme MRG §1 Abs.2: post-1953 new builds, detached single-family homes, holiday lets — freier Mietzins, only contractual indexation. ~50% of AT private rental stock.'
);

-- ============================================================
-- Switzerland (CH) — fix 2024 effective date; split 2025 into two cuts (March and September)
-- ============================================================
UPDATE rent_regulation_rules
SET effective_date = '2023-12-01',
    notes = 'Reference rate raised to 1.75% on 1-Dec-2023; held throughout 2024.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000128';

UPDATE rent_regulation_rules
SET index_value = 1.5000,
    effective_date = '2025-03-03',
    notes = 'Reference rate cut to 1.50% on 3-Mar-2025.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000129';

INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_type, index_name, index_value,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000270',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'CH'),
    2025, 'ALL', 'INDEX_LINKED', 'Hypothekarischer Referenzzinssatz', 1.2500,
    '2025-09-02', 'QUARTERLY',
    'https://www.bwo.admin.ch/de/referenzzinssatz',
    'Reference rate cut to 1.25% on 2-Sep-2025 (lowest since 2008).'
);

-- ============================================================
-- Luxembourg (LU) — restructure: residential leases are CAPITAL_BASED (5% formula), no STATEC auto-index
-- ============================================================
UPDATE rent_regulation_rules
SET property_category = 'RESIDENTIAL',
    max_increase_type = 'CAPITAL_BASED',
    index_name = '5% of revalued invested capital',
    additional_conditions = 'Rent ceiling = 5% p.a. of capital invested (revalued by STATEC construction-cost index, depreciated 2%/yr, energy-class reduction). No automatic CPI clause permitted (Loi 21-09-2006 Art. 3). Biennial revision allowed (Art. 5).',
    source_url = 'https://logement.public.lu/fr/proprietaire/logement-location/faq-bail-a-loyer.html',
    notes = 'Bail à usage d''habitation: no STATEC auto-indexation; 5%-of-capital ceiling retained after Loi du 23 juillet 2024 (proposed reduction to 3.5% dropped).',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'LU');

INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000241',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'LU'),
    2026, 'COMMERCIAL', 'INDEX_LINKED', 'Indice des prix à la consommation (STATEC)',
    '2026-01-01', 'ANNUAL',
    'https://statistiques.public.lu/fr/themes/economie-finances/prix.html',
    'Bail commercial: STATEC CPI auto-indexation permitted unless excluded by contract.'
);

-- ============================================================
-- Sweden (SE) — correct 2022-2025 negotiated outcomes per Hyresgästföreningen
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 1.70,
    source_url = 'https://www.hyresgastforeningen.se/',
    notes = 'Hyresgästföreningen: rikssnitt 1.7% (collective bargaining outcome).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000106';

UPDATE rent_regulation_rules
SET max_increase_percentage = 4.20,
    notes = 'Bruksvärdessystemet: rikssnitt 4.2% (higher reflecting 2022 CPI peak).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000107';

UPDATE rent_regulation_rules
SET max_increase_percentage = 5.10,
    notes = 'Bruksvärdessystemet: rikssnitt 5.1% (Stockholm arbitrator spread 5.0–6.3%).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000108';

UPDATE rent_regulation_rules
SET max_increase_percentage = 4.80,
    notes = 'Bruksvärdessystemet: rikssnitt 4.8% (Stockholm arbitrator higher).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000109';

-- ============================================================
-- Norway (NO) — correct 2022 / 2024 / 2025 KPI values per SSB
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 5.80,
    additional_conditions = '§4-2 KPI-justering: max én gang per 12 mnd, 1 mnd skriftlig varsel. §4-3 markedsleie: tidligst etter 2 år 6 mnd, 6 mnd varsel.',
    notes = 'KPI YoY 2022 ≈ 5.8% (SSB annual avg).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000111';

UPDATE rent_regulation_rules
SET max_increase_percentage = 3.10,
    notes = 'KPI YoY 2024 ≈ 3.1% (SSB annual avg).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000113';

UPDATE rent_regulation_rules
SET max_increase_percentage = 3.50,
    notes = 'KPI YoY 2025 ≈ 3.5% (SSB published rate for 2025 anniversaries).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000114';

-- ============================================================
-- Denmark (DK) — backfill missing percentages; record 2022 statutory 4% cap
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 9.10,
    max_increase_type = 'COST_BASED',
    index_name = 'NPI (Nettoprisindeks)',
    index_value = 9.10,
    additional_conditions = 'Statutory 4% cap applied 2022-2024 (Lov nr. 197/2022) on NPI-linked rent increases.',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI årsgennemsnit +9.1%, but capped at 4% by Lov nr. 197/2022.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000101';

UPDATE rent_regulation_rules
SET max_increase_percentage = 4.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    index_name = 'NPI (capped at 4%)',
    source_url = 'https://www.retsinformation.dk/eli/lta/2022/197',
    notes = 'NPI 2023 ≈ 3.3%, capped at 4% by Lov nr. 197/2022.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000102';

UPDATE rent_regulation_rules
SET max_increase_percentage = 1.40,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'Statutory 4% cap expired 2024-12-31; NPI ≈ 1.4%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000103';

UPDATE rent_regulation_rules
SET max_increase_percentage = 1.40,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI ≈ 1.4% (cost-based system; landlord pass-through documented cost increases).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000104';

UPDATE rent_regulation_rules
SET max_increase_percentage = 2.10,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI April-2026 YoY ≈ 2.1%. Pre-1992 cost-based system per Lejeloven §§ 19-22 (Boligreguleringsloven repealed 2022-07-01).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000105';

-- Add POST_1991 and SMÅHUSE categories for 2026
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_type, effective_date, frequency, source_url, notes
) VALUES
('RRL01J00000000000000000260',
 (SELECT id FROM rent_regulation_countries WHERE country_code='DK'),
 2026, 'POST_1991', 'MARKET',
 '2026-01-01', 'ANNUAL',
 'https://www.dst.dk/',
 'Post-1991 buildings: fri leje per Lejeloven §53 stk. 3-5. Contractual indexation typically NPI-linked.'),
('RRL01J00000000000000000261',
 (SELECT id FROM rent_regulation_countries WHERE country_code='DK'),
 2026, 'SMÅHUSE', 'COMPARATIVE',
 '2026-01-01', 'ANNUAL',
 'https://www.dst.dk/',
 'Småhuse (≤6 units pre-1992): det lejedes værdi rule. Comparable-rent benchmark.');

-- ============================================================
-- Finland (FI) — backfill missing index_name; add ARA social housing category
-- ============================================================
UPDATE rent_regulation_rules
SET index_name = 'elinkustannusindeksi (Tilastokeskus)',
    additional_conditions = 'No statutory cap. AHVL 481/1995 § 27: min 2-month written notice for rent change.',
    notice_period_days = 60,
    source_url = 'https://stat.fi/vuokran-tarkistaminen-elinkustannusindeksilla',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'FI');

INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000230',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'FI'),
    2026, 'ARA_SOCIAL', 'COST_BASED', 'omakustannusvuokra',
    '2026-01-01', 'ANNUAL',
    'https://ohjeet.ara.fi/fi/vuokranmaaritys-hyresbestamning/v4/vuokrantarkistus-elinkustannusindeksilla',
    'ARA-funded social housing: cost-recovery rent (omakustannusperiaate) per ARAVA-laki and AKVL.'
);

-- ============================================================
-- Poland (PL) — fix property categorisation; add Art. 8a 3%-of-reconstruction-value rule
-- ============================================================
-- Add PRIVATE_REGULATED, INSTITUTIONAL, OCCASIONAL categories for 2026
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, notice_period_days, frequency,
    additional_conditions, source_url, notes
) VALUES
('RRL01J00000000000000000220',
 (SELECT id FROM rent_regulation_countries WHERE country_code='PL'),
 2026, 'PRIVATE_REGULATED', 3.00, 'THRESHOLD_PERCENTAGE',
 'wskaźnik przeliczeniowy kosztu odtworzenia 1 m²',
 '2026-01-01', 90, 'SEMIANNUAL',
 'Increases above 3% of reconstruction value challengeable by tenant within 2 months. Min 6 months between increases. Landlord must justify on demand within 14 days.',
 'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
 'Ustawa o ochronie praw lokatorów art. 8a, art. 9. Wskaźnik set per voivodeship every 6 months by wojewoda.'),
('RRL01J00000000000000000221',
 (SELECT id FROM rent_regulation_countries WHERE country_code='PL'),
 2026, 'INSTITUTIONAL', NULL, 'MARKET_RENT', NULL,
 '2026-01-01', NULL, 'ANNUAL', NULL,
 'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
 'Najem instytucjonalny (art. 19f-19j): exempt from art. 8a cap; contractual indexation only.'),
('RRL01J00000000000000000222',
 (SELECT id FROM rent_regulation_countries WHERE country_code='PL'),
 2026, 'OCCASIONAL', NULL, 'MARKET_RENT', NULL,
 '2026-01-01', NULL, 'ANNUAL', NULL,
 'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
 'Najem okazjonalny (art. 19a-19e): exempt from art. 8a cap; notarised eviction declaration required.');

UPDATE rent_regulation_rules
SET notes = 'Mieszkania komunalne / TBS / SIM: rent set by uchwała gminy; subject to wskaźnik-based ceiling (art. 7 ust. 5).'
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'PL')
  AND property_category = 'MUNICIPAL';

-- ============================================================
-- Czech Republic (CZ) — add §2249 NOZ 20%-over-3-years statutory ceiling
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 20.00,
    max_increase_type = 'STATUTORY_CEILING',
    frequency = 'TRIENNIAL',
    notice_period_days = 90,
    additional_conditions = '§2249 NOZ: unilateral landlord proposal capped at 20% over rolling 3 years, up to local market level. Tenant may refuse; court sets price. §2254: deposit ≤ 3× monthly rent. Contractual inflation indexation (ČSÚ CPI) permitted.',
    source_url = 'https://www.zakonyprolidi.cz/cs/2012-89#p2249',
    notes = 'Deregulated 2012. §2249 NOZ caps unilateral increases at 20% / 3 years.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CZ');

-- ============================================================
-- United Kingdom — Scotland 2024/2025 formula-based cap; complete Wales rules; add Northern Ireland
-- ============================================================
-- Scotland 2024: Rent Adjudication Regulations 2024 (tapered formula)
UPDATE rent_regulation_rules
SET effective_date = '2024-04-01',
    max_increase_percentage = 12.00,
    max_increase_type = 'FORMULA_BASED',
    notes = 'Cost of Living rent cap ended 1-Apr-2024. Rent Adjudication (Temporary Modifications) (Scotland) Regulations 2024: market-gap formula = 6% + 0.33% per pp over 6%, capped at 12%.',
    source_url = 'https://www.gov.scot/publications/cost-of-living-rent-and-eviction/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000063';

UPDATE rent_regulation_rules
SET max_increase_percentage = 12.00,
    max_increase_type = 'FORMULA_BASED',
    notes = 'Tapered rent adjudication formula extended to 31-Mar-2025 then status quo restored. Housing (Scotland) Bill in progress.',
    source_url = 'https://spice-spotlight.scot/2025/04/07/rent-adjudication-a-return-to-the-status-quo/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000064';

-- England 2026: enrich Renters' Rights Act conditions
UPDATE rent_regulation_rules
SET additional_conditions = 'Max 1 rent increase per year. Section 13 notice procedure retained (2 months notice). Tenant may challenge at First-tier Tribunal; tribunal cannot exceed landlord''s proposed rent. Periodic assured tenancies replace fixed-term ASTs. Section 21 abolished. Max 1 month rent in advance after agreement signed. Code-registered PBSA exempt from APT regime; private student HMOs in scope.',
    source_url = 'https://www.legislation.gov.uk/ukpga/2025/26/contents',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000070';

-- Wales rules 2022-2026 (previously empty)
INSERT INTO rent_regulation_rules (
    identifier, country_id, region_id, YEAR, property_category,
    max_increase_type, effective_date, frequency,
    additional_conditions, source_url, notes
) VALUES
('RRL01J00000000000000000180',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WLS'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2022, 'ALL', 'MARKET_RENT', '2022-12-01', 'ANNUAL',
 'Renting Homes (Wales) Act 2016 in force 1-Dec-2022. Standard occupation contracts; rent variation requires 2 months written notice. No statutory cap.',
 'https://www.gov.wales/renting-homes',
 'Welsh framework switched from ASTs to standard occupation contracts on 1-Dec-2022.'),
('RRL01J00000000000000000181',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WLS'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2023, 'ALL', 'MARKET_RENT', '2023-01-01', 'ANNUAL',
 'Rent variation requires 2 months written notice. No statutory cap.',
 'https://www.gov.wales/renting-homes',
 'Standard market-based system continues.'),
('RRL01J00000000000000000182',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WLS'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2024, 'ALL', 'MARKET_RENT', '2024-01-01', 'ANNUAL',
 'Rent variation requires 2 months written notice. No statutory cap.',
 'https://www.gov.wales/written-statement-publication-white-paper-adequate-housing-and-fair-rents',
 'October 2024 White Paper: Welsh Government rejected rent controls, citing Scotland evidence.'),
('RRL01J00000000000000000183',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WLS'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2025, 'ALL', 'MARKET_RENT', '2025-01-01', 'ANNUAL',
 'Rent variation requires 2 months written notice. No statutory cap.',
 'https://www.gov.wales/renting-homes',
 'No statutory rent cap. Renters'' Rights Act 2025 extends limited discrimination protections to Wales.'),
('RRL01J00000000000000000184',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='WLS'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2026, 'ALL', 'MARKET_RENT', '2026-01-01', 'ANNUAL',
 'Rent variation requires 2 months written notice. No statutory cap.',
 'https://www.gov.wales/renting-homes',
 'Market-based; no rent control planned.');

-- Northern Ireland: new region + rules
INSERT INTO rent_regulation_regions (identifier, country_id, region_code, region_name, summary)
VALUES (
    'RRG01J00000000000000000016',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'GB'),
    'NIR', 'Northern Ireland',
    'Private Tenancies Act (NI) 2022. Rent may rise once per 12 months with 3 months written notice. No statutory percentage cap.'
);

INSERT INTO rent_regulation_rules (
    identifier, country_id, region_id, YEAR, property_category,
    max_increase_type, effective_date, notice_period_days, frequency,
    additional_conditions, source_url, notes
) VALUES
('RRL01J00000000000000000185',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='NIR'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2023, 'ALL', 'MARKET_RENT', '2023-04-01', 90, 'ANNUAL',
 'Section 7 in force 1-Apr-2023: rent may increase max once per 12 months; 3 months written notice required. 12-month protection from grant of tenancy.',
 'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
 'Private Tenancies Act (NI) 2022 effective 1-Apr-2023.'),
('RRL01J00000000000000000186',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='NIR'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2024, 'ALL', 'MARKET_RENT', '2024-01-01', 90, 'ANNUAL',
 'Max one rent increase per 12 months; 3 months written notice.',
 'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
 'Continued operation of Private Tenancies Act (NI) 2022.'),
('RRL01J00000000000000000187',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='NIR'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2025, 'ALL', 'MARKET_RENT', '2025-01-01', 90, 'ANNUAL',
 'Max one rent increase per 12 months; 3 months written notice.',
 'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
 'No change in 2025.'),
('RRL01J00000000000000000188',
 (SELECT id FROM rent_regulation_countries WHERE country_code='GB'),
 (SELECT id FROM rent_regulation_regions WHERE region_code='NIR'
   AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='GB')),
 2026, 'ALL', 'MARKET_RENT', '2026-01-01', 90, 'ANNUAL',
 'Max one rent increase per 12 months; 3 months written notice. Department reserve power to extend 12-month protection up to 2 years (not yet exercised).',
 'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
 'No statutory percentage cap. Renters'' Rights Act 2025 does not extend to NI (housing is devolved).');

-- ============================================================
-- US — California historical regional CPI values (LA region as canonical, not statutory ceiling)
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 8.60,
    notes = 'LA region Aug-2022 cycle: 5% + 3.6% Apr-Apr CPI = 8.6%. Statewide ceiling 10%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000071';

UPDATE rent_regulation_rules
SET max_increase_percentage = 8.80,
    notes = 'LA/SF Aug-2023 cycle: 5% + 3.8% CPI = 8.8%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000072';

UPDATE rent_regulation_rules
SET max_increase_percentage = 8.90,
    notes = 'LA Aug-2024 cycle: 5% + 3.9% CPI = 8.9%. SF Aug-2024 ≈ 8.9%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000073';

UPDATE rent_regulation_rules
SET max_increase_percentage = 8.00,
    notes = 'LA Aug-2025 cycle: 5% + 3.0% CPI = 8.0%. SF Aug-2025: 6.3%.',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000074';

-- US — New York 2025 actual RGB Order #57 (not the 2.50% projection)
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.00,
    index_name = 'RGB Order #57 (1-year)',
    notes = 'Order #57: 3.00% 1-year, 4.50% 2-year. Adopted 2025-06-30.',
    source_url = 'https://rentguidelinesboard.cityofnewyork.us/2025-26-apartment-loft-order-57/',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000079';

UPDATE rent_regulation_rules
SET notes = 'Preliminary; RGB Order #58 vote scheduled 2026-06-22. Range under discussion: 0–2% (1-year), 0–4% (2-year).',
    updated_at = now()
WHERE identifier = 'RRL01J00000000000000000080';

-- Fix NY 2023 note (2-year was 2.75% for first 12 months + 3.20% for second)
UPDATE rent_regulation_rules
SET notes = 'Order #55: 3.00% 1-year; 2-year = 2.75% (yr 1) + 3.20% (yr 2).',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'US')
  AND YEAR = 2023
  AND region_id = (
    SELECT id FROM rent_regulation_regions WHERE region_code = 'NY'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'US')
  );

-- ============================================================
-- Canada — Quebec TAL: fix 2024 (4.00%) and 2025 (5.90%) basic values
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 4.00,
    additional_conditions = 'Electric-heat: 4.0%. Gas-heat: 4.5%. Oil-heat: 7.3%.',
    notes = 'TAL 2024 base (unheated): 4.0%.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  AND YEAR = 2024
  AND region_id = (
    SELECT id FROM rent_regulation_regions WHERE region_code = 'QC'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  );

UPDATE rent_regulation_rules
SET max_increase_percentage = 5.90,
    additional_conditions = 'Electric-heat: 5.8%. Gas-heat: 7.3%. Largest increase in 3+ decades.',
    notes = 'TAL 2025 base (unheated): 5.9%.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  AND YEAR = 2025
  AND region_id = (
    SELECT id FROM rent_regulation_regions WHERE region_code = 'QC'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  );

-- ============================================================
-- Bump last_reviewed_at on all countries reviewed in this expert validation pass
-- ============================================================
UPDATE rent_regulation_countries
SET last_reviewed_at = '2026-05-14 12:00:00',
    updated_at = now()
WHERE country_code IN (
    'NL','DE','PT','FR','ES','BE','IE','IT','AT','LU',
    'GB','SE','NO','CH','DK','FI','PL','CZ',
    'US','CA'
);
