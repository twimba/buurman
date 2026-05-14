# BUUR-93 — Rent regulation deferred follow-ups: full implementation spec

**Status**: Spec — implementation pending
**Parent**: [BUUR-93](https://linear.app/buurman/issue/BUUR-93/)
**Related**: BUUR-35 (Rent updates central), BUUR-39 (Automated ingestion)
**Last reviewed**: 2026-05-14

---

## 1. Overview

V053 and V054 (committed in `6dd692d0` on branch `update-rent-regulations`) brought rent-regulation data to a defensible state for the 20 countries Buurman currently models, applying both confirmed 2026 values and expert-validation corrections to 2022-2025 historical data. **This spec covers what was deliberately scoped out** of V054 so the consolidation could ship.

Scope of this spec:

1. **Schema evolution** for multi-dimensional regimes (Option C — typed dimensional columns).
2. **DE Länder rollout**: 7 new federal states, ~35 rules.
3. **US state-level**: 11 new regions, ~33 rules.
4. **CA provinces**: 10 new regions, ~50 rules.
5. **FR encadrement des loyers**: 9 zones, ~40 rules + DPE 2026 reform.
6. **CH cantons**: 6 regions, ~6 rules.
7. **ES Catalonia regional + historical pre/post Ley 12/2023 split**: 1 region, ~13 rules.
8. **Source URL backfill** for Nordics + Eastern Europe + IT/AT historical rows.
9. **Mid-2026 follow-up data refreshes** (US-NY, FR Q2, US-CA, NL).

Estimated effort: **8-10 engineering days** total across migrations, repository changes, OpenAPI, and frontend.

---

## 2. Migration plan

Migrations land in order. Each is independently safe to merge; later migrations depend on schema added by earlier ones.

| Migration | Purpose | Effort |
|-----------|---------|--------|
| **V055** | Add typed dimensional columns to `rent_regulation_rules` (additive, nullable) | 0.5d |
| **V056** | Backfill existing rows by parsing compound `property_category` values into new columns | 1d |
| **V057** | DE Länder regions + rules | 0.5d |
| **V058** | CA provinces regions + rules | 0.5d |
| **V059** | US sub-state regions + rules | 1d |
| **V060** | FR encadrement des loyers zones + rules + DPE 2026 reform | 1d |
| **V061** | CH cantons (GE LDTR, VD formule officielle, BS WRFG, ZH/BE/BL market) | 0.25d |
| **V062** | ES Catalonia regional + pre/post Ley 12/2023 historical split | 0.5d |
| **V063** | Source URL backfill for Nordics + Eastern EU + IT/AT historical rows | 0.5d |
| **V064** | CHECK constraints on dimensional columns (build_year ranges, EPC class) | 0.25d |
| **V065** | Drop deprecated `sector` column; rename `property_category` → `property_type` (or similar after backfill) | 0.5d |
| Repo/API/UI | `RentRegulationRepository` + domain + DTO + OpenAPI + frontend types | 1.5d |
| BUUR-39 link | Update `RentRegulationService.resolveApplicableRule()` to use specificity-ranked lookup | 0.5d |

Mid-2026 data refreshes (V066+) ship as point updates when external sources publish — not blocking this ticket.

---

## 3. Schema evolution (V055-V056)

### Problem

`rent_regulation_rules` has a single `property_category` slot per row, but real-world regimes are multi-dimensional. The slot has been overloaded with values from at least 8 independent axes (regulatory regime, property type, contract type, tax regime, building era, EPC class, tenancy phase, contract-date threshold). V054 worked around this with text in `additional_conditions` for some fields, which is not queryable.

### Decision: Option C — typed dimensional columns

Selected over Option A (modifiers table — N-way join overhead, not JOOQ-friendly) and Option B (JSONB conditions — Buurman avoids JSONB after removing `teams.settings` in favor of `team_preferences`). Matches existing pattern of explicit typed columns.

### V055 — schema additive

```sql
ALTER TABLE rent_regulation_rules
    -- Regulatory regime (was overloaded into property_category)
    ADD COLUMN regime VARCHAR(40),
    -- Property type (formerly conflated with regime)
    ADD COLUMN property_type VARCHAR(40),
    -- Contract form (canone libero vs concordato, institutional/occasional, AST/SOC vs assured periodic)
    ADD COLUMN contract_type VARCHAR(40),
    -- Tax regime (Italian cedolare secca overrides indexation)
    ADD COLUMN tax_regime VARCHAR(40),
    -- Existing vs new lease, on-relet, renewal
    ADD COLUMN tenancy_phase VARCHAR(20),
    -- Build-year window (e.g. DK pre/post-1992, AT pre/post-1953)
    ADD COLUMN build_year_min INT,
    ADD COLUMN build_year_max INT,
    -- EPC class window (e.g. BE EPC-tiered freeze, FR DPE F/G)
    ADD COLUMN epc_class_min CHAR(1),
    ADD COLUMN epc_class_max CHAR(1),
    -- Contract signature date window (ES pre/post Ley 12/2023 26-May-2023)
    ADD COLUMN contract_signed_after DATE,
    ADD COLUMN contract_signed_before DATE,
    -- Landlord-size threshold (ES gran tenedor)
    ADD COLUMN landlord_min_properties INT,
    -- Region-specific area code (e.g. FR encadrement zone within région)
    ADD COLUMN area_code VARCHAR(40);

CREATE INDEX idx_rent_reg_rules_dimensions
    ON rent_regulation_rules (country_id, year, regime, property_type, contract_type);
```

### V056 — data backfill from compound `property_category`

```sql
-- NL middenhuur — already typed, just propagate
UPDATE rent_regulation_rules
SET regime = 'MIDDLE_RENT', property_type = 'RESIDENTIAL'
WHERE property_category = 'MIDDLE_RENT';

UPDATE rent_regulation_rules
SET regime = 'REGULATED', property_type = 'RESIDENTIAL'
WHERE property_category = 'REGULATED';

UPDATE rent_regulation_rules
SET regime = 'FREE_SECTOR', property_type = 'RESIDENTIAL'
WHERE property_category = 'FREE_SECTOR';

-- DK build-era split
UPDATE rent_regulation_rules SET build_year_max = 1991, property_type = 'RESIDENTIAL'
WHERE property_category = 'PRE_1992';

UPDATE rent_regulation_rules SET build_year_min = 1992, property_type = 'RESIDENTIAL'
WHERE property_category = 'POST_1991';

UPDATE rent_regulation_rules
SET regime = 'SMÅHUSE', property_type = 'RESIDENTIAL', build_year_max = 1991
WHERE property_category = 'SMÅHUSE';

-- FI social housing
UPDATE rent_regulation_rules
SET property_type = 'SOCIAL', regime = 'COST_BASED'
WHERE property_category = 'ARA_SOCIAL';

-- FR DPE F/G freeze
UPDATE rent_regulation_rules
SET epc_class_min = 'F', epc_class_max = 'G', property_type = 'RESIDENTIAL'
WHERE property_category = 'DPE_F_OR_G';

-- FR DOM/Corse
UPDATE rent_regulation_rules SET area_code = 'DOM', property_type = 'RESIDENTIAL'
WHERE property_category = 'DOM';

UPDATE rent_regulation_rules SET area_code = 'CORSE', property_type = 'RESIDENTIAL'
WHERE property_category = 'CORSE';

-- ES contract-date threshold
UPDATE rent_regulation_rules
SET contract_signed_after = '2023-05-26', property_type = 'RESIDENTIAL'
WHERE property_category = 'CONTRACT_POST_2023_05_26';

UPDATE rent_regulation_rules
SET contract_signed_before = '2023-05-26', property_type = 'RESIDENTIAL'
WHERE property_category = 'CONTRACT_PRE_LEY_VIVIENDA';

-- ES zonas tensionadas / gran tenedor
UPDATE rent_regulation_rules
SET regime = 'ZONA_TENSIONADA',
    landlord_min_properties = 5,
    property_type = 'RESIDENTIAL'
WHERE property_category = 'ZONA_TENSIONADA_GRAN_TENEDOR';

UPDATE rent_regulation_rules
SET property_type = 'SEASONAL'
WHERE property_category = 'SEASONAL';

-- IT canone libero
UPDATE rent_regulation_rules
SET regime = 'FREE_MARKET', contract_type = 'CANONE_LIBERO', property_type = 'RESIDENTIAL'
WHERE property_category = 'FREE_MARKET' AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='IT');

-- AT Vollausnahme
UPDATE rent_regulation_rules
SET regime = 'FREE_MARKET', property_type = 'RESIDENTIAL', build_year_min = 1953
WHERE property_category = 'FREE_MARKET' AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='AT');

-- LU residential vs commercial
UPDATE rent_regulation_rules SET property_type = 'RESIDENTIAL'
WHERE property_category = 'RESIDENTIAL';

UPDATE rent_regulation_rules SET property_type = 'COMMERCIAL'
WHERE property_category = 'COMMERCIAL';

-- DE Mietpreisbremse new-lease vs existing-lease (Kappungsgrenze)
UPDATE rent_regulation_rules
SET tenancy_phase = 'NEW_LEASE', property_type = 'RESIDENTIAL'
WHERE property_category = 'NEW_LEASE';

UPDATE rent_regulation_rules
SET tenancy_phase = 'MODERNISATION', property_type = 'RESIDENTIAL'
WHERE property_category = 'MODERNISATION';

-- Default: 'ALL' → property_type = 'RESIDENTIAL' (most common)
UPDATE rent_regulation_rules SET property_type = 'RESIDENTIAL'
WHERE property_type IS NULL AND property_category = 'ALL';

-- IT cedolare secca already in additional_conditions; parse into tax_regime column
UPDATE rent_regulation_rules
SET tax_regime = 'NON_CEDOLARE_SECCA'
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code='IT')
  AND additional_conditions ILIKE '%cedolare secca%';
```

### Rule-resolution algorithm

```sql
SELECT * FROM rent_regulation_rules r
WHERE r.country_id = :country_id
  AND (r.region_id = :region_id OR r.region_id IS NULL)
  AND r.year = :year
  AND (r.regime IS NULL OR r.regime = :lease_regime)
  AND (r.property_type IS NULL OR r.property_type = :property_type)
  AND (r.contract_type IS NULL OR r.contract_type = :contract_type)
  AND (r.tax_regime IS NULL OR r.tax_regime = :tax_regime)
  AND (r.tenancy_phase IS NULL OR r.tenancy_phase = :tenancy_phase)
  AND (r.build_year_min IS NULL OR :build_year >= r.build_year_min)
  AND (r.build_year_max IS NULL OR :build_year <= r.build_year_max)
  AND (r.epc_class_min IS NULL OR :epc_class >= r.epc_class_min)
  AND (r.epc_class_max IS NULL OR :epc_class <= r.epc_class_max)
  AND (r.contract_signed_after IS NULL OR :contract_signed_at >= r.contract_signed_after)
  AND (r.contract_signed_before IS NULL OR :contract_signed_at < r.contract_signed_before)
  AND (r.landlord_min_properties IS NULL OR :landlord_property_count >= r.landlord_min_properties)
  AND (r.area_code IS NULL OR r.area_code = :area_code)
ORDER BY
    -- Specificity: rules with more non-NULL filters win
    ((CASE WHEN region_id              IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN regime                 IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN contract_type          IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN tax_regime             IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN tenancy_phase          IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN build_year_min         IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN build_year_max         IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN epc_class_min          IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN epc_class_max          IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN contract_signed_after  IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN landlord_min_properties IS NOT NULL THEN 1 ELSE 0 END) +
     (CASE WHEN area_code              IS NOT NULL THEN 1 ELSE 0 END)) DESC,
    r.effective_date DESC
LIMIT 1;
```

Implement as `RentRegulationService.resolveApplicableRule(country, region, year, leaseAttributes)`.

---

## 4. Identifier allocation

To avoid collisions across data migrations, identifier ranges are reserved:

| Range | Region |
|-------|--------|
| `RRG01J0…030`-`036` | DE Länder (7) |
| `RRG01J0…040`-`04A` | US sub-state (11) |
| `RRG01J0…050`-`059` | CA provinces (10) |
| `RRG01J0…060`-`068` | FR encadrement zones (9) |
| `RRG01J0…070`-`075` | CH cantons (6) |
| `RRG01J0…080` | ES Catalonia (1) |
| `RRL01J0…300`-`334` | DE Länder rules (35) |
| `RRL01J0…400`-`432` | US sub-state rules (33) |
| `RRL01J0…500`-`549` | CA province rules (50) |
| `RRL01J0…600`-`681` | FR encadrement rules (~40) |
| `RRL01J0…700`-`750` | CH canton rules (6) |
| `RRL01J0…800`-`822` | ES Catalonia rules (7) |
| `RRL01J0…900`-`905` | ES historical pre/post Ley 12/2023 (6) |

**Note**: US `RRG01J0…04A` and `RRL01J0…XXX` ranges use hex-style extension where digit space runs out. Easier alternative: allocate by hundreds. Final assignment up to implementer; the ranges above prevent overlap.

---

## 5. V057 — Germany Länder rollout

### Federal context

Bundestag (KW26/2025, 26 June 2025) extended Mietpreisbremse §556d BGB to **31 December 2029**. Bundesrat confirmed July 2025. Each Land Verordnung must align.

### 7 new regions

| Region | Code | Verordnung | In force | Designated municipalities | Notable cities |
|--------|------|-----------|----------|----------------------------|-----------------|
| Nordrhein-Westfalen | `NW` | Mieterschutzverordnung NRW (28.01.2025) | until 2029-12-31 (KG 2030-02-28) | 57 | Köln, Düsseldorf, Bonn, Münster, Aachen |
| Hessen | `HE` | Hessische Mieterschutzverordnung 18.11.2020 | until 2026-11-25 (extension to 2029 planned) | 49 | Frankfurt, Wiesbaden, Darmstadt, Offenbach, Marburg |
| Baden-Württemberg | `BW` | Mietpreisbegrenzungsverordnung BW (GBl. 2025 Nr. 144) + Kappungsgrenzenverordnung | 2026-01-01 → 2026-12-31 (alignment pending) | 130 | Stuttgart, Karlsruhe, Heidelberg, Freiburg, Tübingen |
| Brandenburg | `BB` | MietbegrenzV + KappVO (Kabinett 25.11.2025) | 2026-01-01 → 2029-12-31 | 36 | Potsdam-Umland: Teltow, Kleinmachnow, Stahnsdorf, Falkensee, Königs Wusterhausen, Oranienburg |
| Niedersachsen | `NI` | Niedersächsische Mieterschutzverordnung (Änderung 04.12.2025) | until 2029-12-31 | 57 | Hannover, Braunschweig, Göttingen, Oldenburg, Osnabrück |
| Rheinland-Pfalz | `RP` | Landesverordnung 16.09.2025 | 2025-10-08 → 2029-12-31 | 7 (5 cities + 2 Landkreise) | Mainz, Ludwigshafen, Speyer, Worms, Landau |
| Schleswig-Holstein | `SH` | Kappungsgrenzenverordnung 01.05.2024 (no Mietpreisbremse) | 2024-05-01 → 2029-04-30 | 62 | Kiel, Lübeck, Flensburg, Sylt, Ahrensburg |

### Rule shape (per region × year 2022-2026)

Each row uses `tenancy_phase = 'EXISTING_LEASE'`, `regime = 'KAPPUNGSGRENZE'`, `frequency = 'TRIENNIAL'`, `max_increase_type = 'STATUTORY_CAP'`, `index_name = 'Kappungsgrenze (tight market)'`, `max_increase_percentage = 15.00`.

NRW also needs the new-lease Mietpreisbremse rule (Mietspiegel + 10%) at the regional level for completeness; pattern matches the national rule added in V054.

### Sources

- https://www.bundestag.de/dokumente/textarchiv/2025/kw26-de-mietpreisbremse-1084786 (federal extension)
- https://recht.nrw.de/lmi/owa/br_vbl_detail_text?anw_nr=6&vd_id=22119 (NW)
- https://wirtschaft.hessen.de/Mieterschutzverordnung-vom-18-November-2020-GVBl-S-802 (HE)
- https://mlw.baden-wuerttemberg.de/de/bauen-wohnen/wohnungsbau/mietpreisbremse (BW)
- https://mil.brandenburg.de/mil/de/presse/detail/~25-11-2025-kabinett-mietpreisbremse-und-kappungsgrenzenverordnung (BB)
- https://www.mw.niedersachsen.de/startseite/bauen_wohnen/soziales_wohnungswesen/niedersachsische_mieterschutzverordnung/ (NI)
- https://fm.rlp.de/themen/bauen-und-wohnen/rheinland-pfaelzische-mieterschutzregelungen (RP)
- https://www.schleswig-holstein.de/DE/landesregierung/ministerien-behoerden/IV/Presse/PI/2024/240319_kappungsgrenzenverordnung (SH)

---

## 6. V058 — Canadian provinces rollout

### 10 new regions

| Region | Code | Body | Statute | 2026 cap | Period |
|--------|------|------|---------|----------|--------|
| Manitoba | `MB` | RTB Manitoba | RTA C.C.S.M. c. R119 Part 9 | **1.8%** | Calendar year |
| New Brunswick | `NB` | Service NB Tribunal | RTA RSNB 1973 c R-10.2 | **3.0%** | Apr 1 → Mar 31 |
| Nova Scotia | `NS` | Service NS | Interim Cap Act SNS 2021 c 22 | **5.0%** | Calendar year, until 2027-12-31 |
| Prince Edward Island | `PE` | IRAC | RTA RSPEI 1988 c R-13.11 | **2.0%** | Calendar year |
| Newfoundland & Labrador | `NL` | Service NL | RTA, 2018 SNL 2018 c R-14.2 | **MARKET** | n/a |
| Alberta | `AB` | RTDRS | RTA SA 2004 c R-17.1 | **MARKET** | n/a |
| Saskatchewan | `SK` | Office of RT | RTA, 2006 SS 2006 c R-22.0001 | **MARKET** | n/a |
| Yukon | `YT` | RT Office | RLTA SY 2012 c 20 | **MARKET** | n/a |
| Northwest Territories | `NT` | Rental Officer | RTA RSNWT 1988 c R-5 | **MARKET** | n/a |
| Nunavut | `NU` | Rental Officer | RTA RSNWT (Nu) 1988 c R-5 | **MARKET** | n/a |

### Historical year-by-year

| Region | 2022 | 2023 | 2024 | 2025 | 2026 |
|--------|------|------|------|------|------|
| MB | 0.0% | 0.0% | 3.0% | 1.7% | **1.8%** |
| NB | MARKET | 3.8% (1y cap) | 3.0% | 3.0% | **3.0%** |
| NS | 2.0% | 2.0% | 5.0% | 5.0% | **5.0%** |
| PE | 1.0% | 0.0% (heated) | 3.0% | 3.0% | **2.0%** |
| NL/AB/SK/YT/NT/NU | MARKET | MARKET | MARKET | MARKET | MARKET |

### Exemptions worth recording

- MB: Buildings first occupied after 2001-04-09; high-rent threshold (2026: $1,615/mo); personal-care homes; non-profit/co-op.
- NS: Cap applies to same tenant only; new rentals exempt.
- PE: Above-guideline applications permitted (capital cost / tax increase).
- NB: Above-cap up to 9% with renovation justification.

### Sources

Per province in agent transcript at `/private/tmp/claude-504/.../tasks/a42b0e1b856879cd9.output`. Primary URLs: gov.mb.ca/cca/rtb, www2.gnb.ca, novascotia.ca, peirentaloffice.ca, gov.nl.ca/dgsnl, alberta.ca/rent-increases, saskatchewan.ca, yukon.ca, justice.gov.nt.ca, gov.nu.ca/justice.

---

## 7. V059 — US sub-state rollout

### 11 new regions

| Region | Code | Mechanism | 2026 cap |
|--------|------|-----------|----------|
| Massachusetts (statewide) | `MA` | Ch. 40P (1994) ban; 2026 ballot Init. 25-21 pending | **MARKET** (informational region) |
| Saint Paul, MN | `MN-STPAUL` | Voter-approved Rent Stab. Ordinance (2021), 2024 amendments | **3.0%** flat |
| Portland, ME | `ME-PORTLAND` | Citizen Initiative 1 (2020): 70% of Greater Boston CPI-U | **2.2%** |
| New Jersey (statewide) | `NJ` | No statewide; ~100 municipal ordinances | **MARKET** (informational) |
| Newark, NJ | `NJ-NEWARK` | Rent Control Ordinance: lower of 4% or CPI | **4.0% cap** |
| Jersey City, NJ | `NJ-JERSEYCITY` | Rent Control Ordinance: lower of 4% or CPI; 5+ units | **4.0% cap** |
| Hoboken, NJ | `NJ-HOBOKEN` | Rent Leveling Office: lower of 5% or CPI | **5.0% cap** |
| District of Columbia | `DC` | Rental Housing Act 1985: CPI-W + 2% (max 10%) / Senior CPI-W or 5% / Vacancy 10% | Standard **4.1%**, Senior **2.1%** |
| Montgomery County, MD | `MD-MONTGOMERY` | HOME Act 2024-07-23: CPI-U + 3% (max 6%); buildings <23y exempt | **5.2%** |
| Prince George's County, MD | `MD-PRINCEGEORGES` | Permanent Stab. Act 2024-10-17: CPI-U + 3% (max 6%); Senior 4.5% | Standard **5.2%**, Senior **2.2%** |
| Takoma Park, MD | `MD-TAKOMAPARK` | Ch. 6.20: 100% Wash-Balt CPI-U (Mar-Mar) | **~2.2-2.5%** (pending publication) |

### Historical year-by-year (capped jurisdictions)

| Region | 2022 | 2023 | 2024 | 2025 | 2026 |
|--------|------|------|------|------|------|
| MN-STPAUL | 3.0% | 3.0% | 3.0% | 3.0% | **3.0%** |
| ME-PORTLAND | 4.3% | 7.0% | 2.0% | 2.5% | **2.2%** |
| DC-STANDARD | 6.2% | 8.9% | 6.0% (capped) | 4.1% | **4.1%** |
| DC-ELDERLY | 4.2% | 5.0% | 4.0% | 2.5% | **2.1%** |
| MD-MONTGOMERY | — | — | 6.0% (2024-07-23) | 5.7% | **5.2%** |
| MD-PRINCEGEORGES | — | — | 6.0% (2024-10-17) | 5.7% | **5.2%** |
| MD-TAKOMAPARK | — | — | — | 2.4% | TBD |

### Key exemptions

- **MN-STPAUL**: Permanent exemption for buildings constructed after 2004 (post-2024 amendment). Vacancy decontrol permitted.
- **MD-MONTGOMERY**: Buildings <23 years old exempt.
- **DC**: Single-family / owner-occupied <4 units exempt.

### Notes

- **MA**: 2026 ballot Initiative Petition 25-21 (lower of 5% or CPI) gathered 124,000+ signatures. Legislature deadline May 5, 2026. Track for Nov 2026 ballot decision.
- **NJ**: 100+ municipal ordinances. Recommend NJ umbrella region for "rules vary by municipality" + 3 large cities. Add others (Paterson, Elizabeth, Bayonne, Union City) in a future ticket if user demand emerges.

### Sources

Full URLs in agent transcript at `/private/tmp/claude-504/.../tasks/a105bf5ec6f2b48a4.output`. Primary: ballotpedia.org (MA), stpaul.gov, portlandmaine.gov, jerseycitynj.gov, hobokennj.gov, rhc.dc.gov, montgomerycountymd.gov, princegeorgescountymd.gov, takomaparkmd.gov.

---

## 8. V060 — France encadrement des loyers + DPE 2026 reform

### Statutory horizon

Encadrement experimentation under art. 140 of loi ELAN, extended by loi 3DS and loi 27-07-2023, expires **2026-11-23** unless renewed by new primary legislation.

### 9 active zones

| Zone | Code | Start | Decree | Municipalities |
|------|------|-------|--------|----------------|
| Paris | `FR-PARIS` | 2019-07-01 | Décret 2019-315 | 1 (intramuros) |
| Lille MEL | `FR-LILLE-EPCI` | 2020-03-01 | Décret 2020-41 | 3 (Lille, Hellemmes, Lomme) |
| Plaine Commune | `FR-PLAINE-COMMUNE` | 2021-06-01 | Décret 2020-1619 | **9** (incl. L'Île-Saint-Denis) |
| Métropole Lyon | `FR-LYON` | 2021-11-01 | Décret 2021-1143 | 2 (Lyon, Villeurbanne) |
| Est Ensemble | `FR-EST-ENSEMBLE` | 2021-12-01 | Décret 2021-688 | 9 |
| Montpellier | `FR-MONTPELLIER` | 2022-07-01 | Décret 2021-1144 | 1 |
| Bordeaux | `FR-BORDEAUX` | 2022-07-15 | Décret 2021-1145 | 1 |
| Pays Basque | `FR-PAYS-BASQUE` | 2024-11-25 | Décret 2023-981 + arrêté 21-10-2024 | 24 |
| Grenoble-Alpes Métropole | `FR-GRENOBLE-METRO` | 2025-01-20 | Arrêté préfectoral Isère 11-12-2024 | **21** (13 full + 8 partial) |

**Total: 9 EPCI/villes, ~71 communes covered**.

Rules use `regime = 'CEILING_RENT'`, `max_increase_type = 'CEILING_RENT'`, `index_name = 'Loyer de référence + 20%'`. Each zone gets a row per year from start_date through 2026 (capped at 2026-11-23 expiry).

### DPE 2026 coefficient reform

**Citation**: Arrêté du 13 août 2025 (JORF n° 0198, 26-08-2025) modifying arrêté du 31-03-2021.

- Conversion factor electricity final→primary energy: **2.3 → 1.9**, effective for any DPE issued from **2026-01-01**.
- ~**850,000 dwellings** exit F/G classification (ADEME baseline).
- Existing pre-2026 DPEs remain valid; owners may regenerate free via ADEME Observatoire DPE-Audit.
- **Effect on gel des loyers (art. 17-1 loi 89-462)**: F/G ban on IRL revision auto-lifts for properties moving out of F/G under the new coefficient.
- G-letting ban (since 2025-01-01) and F-letting ban (from 2028) **mechanically attenuated**.

Update the existing FR `DPE_F_OR_G` rule notes + add a 2026 row reflecting the coefficient change.

### Sources

Decrees on Légifrance (linked above). Government summary: ecologie.gouv.fr, economie.gouv.fr, service-public.gouv.fr. Full URLs in transcript at `/private/tmp/claude-504/.../tasks/a74a33712cae372ac.output`.

### Renewal watch

If encadrement is not extended by November 2026, all CEILING_RENT rules need an `effective_end = 2026-11-23` and follow-up tracking issue. Several agglomérations (Strasbourg, Nantes, Rennes, Annecy, La Rochelle, Chambéry) applied to join in a 2025 wave but their arrêtés had not entered into force as of 2026-05-14 — they are in the broader "zone tendue" only.

---

## 9. V061 — Swiss cantons + Basel-Stadt WRFG

### 6 new regions

| Canton | Code | Mechanism | 2026 rule |
|--------|------|-----------|-----------|
| Genève | `GE` | LDTR (RSG L 5 20) + formule officielle obligatoire | `CANTONAL_RESTRICTION`: 3-5y rent control on renovated/new-build; pass-through caps CHF 80/120/160 (2/3/4-room); formule officielle since 2025-10-01 must show ref. mortgage rate + CPI; non-compliance = rent null |
| Vaud | `VD` | Formule officielle obligatoire (all districts except Aigle 2026) | `CANTONAL_RESTRICTION`: notification + 30-day contest right; no rent cap beyond federal |
| Zürich | `ZH` | None (housing-protection initiative pending) | `MARKET` |
| Bern | `BE` | None at canton level (Bern city art. 16b is municipal) | `MARKET` |
| Basel-Stadt | `BS` | WRFG (2013) + WRSchV (2022) — similar caps to GE | `CANTONAL_RESTRICTION`: permit required for demolition/conversion; pass-through caps CHF 80/120/160; formule officielle |
| Basel-Landschaft | `BL` | None | `MARKET` |

### Sources

- ge.ch/legislation/rsg/f/rsg_l5_20.html (GE LDTR)
- justice.ge.ch (formule officielle 2025-10-01)
- vd.ch (VD form)
- cvi.ch (VD Aigle 2026 exemption)
- gesetzessammlung.bs.ch (BS WRFG)
- ey.com — comparative analysis of BS and GE

---

## 10. V062 — Spain Catalonia regional + pre/post Ley 12/2023 historical split

### Part A: Catalonia regional

1 new region: `CAT` (Catalunya), country `ES`, effective from **2024-03-14** (initial 140-municipality declaration).

**Resolution TER/2408/2024 (BOE 2024-10-09, BOE-A-2024-20576)**: +131 municipalities → **271 total** (Barcelona, L'Hospitalet, Badalona, Sabadell, Terrassa, Girona, Tarragona, Lleida, Reus, Mataró, Santa Coloma…). Covers 7,172,196 residents = 90.7% of Catalan population. 3-year validity.

**IRPL (Índex de Referència de Preus del Lloguer)**: Catalan-specific reference index. Feb 2025 → Feb 2026 update: **+2.32%**.

**Llei 11/2025 (Parlament 18-12-2025, DOGC 31-12-2025, in force 2026-01-01)**: closes seasonal/room-rental loophole — IRPL caps now apply. **PP filed Tribunal Constitucional challenge** in early 2026 (pending).

**Gran tenedor Catalan threshold (Decret-Llei 1/2025 + 2/2025, in force 2025-01-01)**: lowered to **5+ dwellings in a zona tensionada** (vs. state 10+).

### Part B: Pre/post Ley 12/2023 historical split

V054 added the `CONTRACT_POST_2023_05_26` category only for 2026. Backfill 2023-2025 with both regimes as queryable rows.

| Year | Pre-Ley (`contract_signed_before = '2023-05-26'`) | Post-Ley (`contract_signed_after = '2023-05-26'`) |
|------|---------------------------------------------------|---------------------------------------------------|
| 2023 | **2.00%** (RDL 20/2022, `min(IGC, 2%)`) | **2.00%** (transitional pre-IRAV) |
| 2024 | **3.00%** (RDL 7/2023 + Ley 12/2023 DF 6ª) | **3.00%** (transitional pre-IRAV) |
| 2025 | **NULL** (no statutory cap; contractual IPC default) | **IRAV** monthly (range 1.98-2.32%, avg ~2.16%; Dec 2025 = 2.32%) |

After V055-V056 schema work, these become 6 rows using the new `contract_signed_after` / `contract_signed_before` columns.

### Sources

- boe.es/buscar/act.php?id=BOE-A-2023-12203 (Ley 12/2023)
- boe.es/diario_boe/txt.php?id=BOE-A-2024-20576 (Resolution TER/2408/2024)
- agenciahabitatge.gencat.cat/indexdelloguer/ (IRPL portal)
- catalannews.com, thelocal.es (Llei 11/2025)
- ine.es/uc/oC7D0Ncd, idealista.com (IRAV publication)

---

## 11. V063 — Source URL backfill for historical rows

V029 wrote zero `source_url` for any 2022-2025 row in: **DK, SE, NO, FI, PL, CZ, IT, AT, CH** (only V053/V054 filled URLs for rows they touched). Backfill the official source for each historical row.

Authoritative URLs per country:

- **DK**: https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks (NPI), https://www.retsinformation.dk/eli/lta/2022/197 (2022 cap law)
- **SE**: https://www.hyresgastforeningen.se/om-oss/vad-vi-gor/hyresforhandling/ (per year)
- **NO**: https://www.ssb.no/priser-og-prisindekser/konsumpriser/statistikk/konsumprisindeksen
- **FI**: https://stat.fi/vuokran-tarkistaminen-elinkustannusindeksilla
- **PL**: https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733 (Ustawa o ochronie praw lokatorów)
- **CZ**: https://www.zakonyprolidi.cz/cs/2012-89 (občanský zákoník)
- **IT**: https://www.confedilizia.it/locazioni/indice-istat/
- **AT**: https://www.statistik.at/statistiken/bevoelkerung-und-soziales/wohnen/richtwerte-und-kategoriebetraege
- **CH**: https://www.bwo.admin.ch/de/referenzzinssatz

Single UPDATE per country setting `source_url` on all historical (2022-2025) rows where it is currently NULL.

---

## 12. Mid-2026 follow-up data refreshes (V066+)

Track separately as a "rolling data update" sub-ticket. Not blocking BUUR-93 spec completion.

| Source | Publishes | Action |
|--------|-----------|--------|
| US-NY RGB Order #58 | 2026-06-22 vote | Update NY 2026 row when final |
| FR IRL T2 2026 | 2026-07-10 (INSEE) | Update FR Q2 row |
| US-CA AB 1482 full state coverage | June/July 2026 (April CPI release) | Refine per-region 2026-08-01 cycle values |
| NL 2027 huurverhoging | Typically December 2026 | Add 2027 rules |
| Encadrement des loyers FR | Authorisation expires 2026-11-23 | Track renewal legislation; set `effective_end` if not renewed |

---

## 13. Repository / API / UI changes

### Backend

- `RentRegulationRepository`:
  - Add `Field<>` constants for the 13 new dimensional columns.
  - Add `findApplicableRules(country, region, year, leaseAttributes)` method using the specificity-ranked query.
- `RentRegulationRule` domain (POJO in `buurman-common`):
  - Add `Optional<String> regime`, `Optional<String> propertyType`, etc. (per Buurman's `@Nullable` → `Optional` policy in memory).
- `RentRegulationRuleResponse` DTO (record):
  - Expose the new fields.
- `RentRegulationMapper` (MapStruct):
  - Map new columns from record to domain to DTO.
- `RentRegulationService`:
  - New `resolveApplicableRule()` service method.
- `RentRegulationController`:
  - New endpoint `GET /api/v1/rent-regulations/applicable?country_code=...&region_code=...&year=...&...` returning the most specific matching rule.

### OpenAPI

- Add new optional properties to `RentRegulationRuleResponse` schema.
- Add new endpoint `/rent-regulations/applicable` and its query parameters.
- Run `make bundle-openapi` after editing `openapi/src/`.

### Frontend

- `frontend/app/src/types/rentRegulation.ts`: extend type with optional dimensional fields.
- Update any UI presenting rules to show the new columns where populated.

---

## 14. Acceptance criteria

- [ ] **V055** lands with all dimensional columns nullable; existing data unaffected.
- [ ] **V056** backfills existing rows; verify by spot-checking that BE Flanders 2022, NL middenhuur 2026, IT canone libero, AT Vollausnahme, ES post-Ley 2026, DK pre-1992 rows have populated dimensional columns.
- [ ] **V057-V062** land with all 43 new regions and ~140 new rules.
- [ ] **V063** backfills source URLs; no NULL `source_url` remains on 2022-2026 rows.
- [ ] **V064** CHECK constraints in place.
- [ ] **V065** legacy `sector` column dropped; `property_category` retained (deprecated) for one further release.
- [ ] `RentRegulationService.resolveApplicableRule()` returns the correct rule for spot test cases:
  - NL property regulated under Wet betaalbare huur (middenhuur tier) → MIDDLE_RENT row.
  - IT canone libero contract under cedolare secca → 0% indexation row.
  - BE Brussels property with EPC class F in 2023 → 0% freeze row.
  - ES contract signed 2024-01-15 in zona tensionada by gran tenedor → SERPAVI row.
  - DE Berlin existing lease in 2026 → Kappungsgrenze 15%/3y row.
  - DE Berlin new lease in 2026 → Mietpreisbremse Mietspiegel + 10% row.
- [ ] BUUR-93 root checklist complete.
- [ ] No JOOQ regen errors on `mvn generate-sources -pl buurman-jooq -am`.

---

## 15. Out of scope

Tracked separately or deferred to future tickets:

- BUUR-39: automated ingestion from government APIs (uses the schema built here).
- Cambridge MA / Boston MA ballot outcomes (Nov 2026).
- Sub-municipal NJ ordinances beyond Newark/Jersey City/Hoboken.
- Other CA municipal ordinances (Berkeley, SF, Oakland, Santa Monica) beyond the existing CA region rule.
- Sweden Stockholm arbitrator outcomes as separate sub-region (would require region splitting).
- Detailed Italian regional accordi territoriali for canone concordato (per-city ISTAT % differs).
- French DPE coefficient mid-2027 follow-up if EU EED v3 lands.

---

## 16. References

- Git: branch `update-rent-regulations`, commit `6dd692d0` (V053 + V054).
- Migrations: `backend/buurman-jooq/src/main/resources/db/migration/V029__rent_regulation_support.sql`, `V053__*`, `V054__*`.
- Linear: [BUUR-93](https://linear.app/buurman/issue/BUUR-93/), parent [BUUR-35](https://linear.app/buurman/issue/BUUR-35/).
- Agent research transcripts (full SQL bodies, all source URLs):
  - DE Länder: `/private/tmp/claude-504/.../tasks/ad0de08dc199c5d4b.output`
  - CA provinces: `/private/tmp/claude-504/.../tasks/a42b0e1b856879cd9.output`
  - US sub-state: `/private/tmp/claude-504/.../tasks/a105bf5ec6f2b48a4.output`
  - FR encadrement + DPE: `/private/tmp/claude-504/.../tasks/a74a33712cae372ac.output`
  - CH cantons + ES Catalonia: `/private/tmp/claude-504/.../tasks/a0ac5818d8d1fd0e1.output`
  - ES historical split: `/private/tmp/claude-504/.../tasks/abf49d7cdf9ce5519.output`
