# Lease agreement documents for every rent-regulation country (+ Greece)

**Date:** 2026-10-04
**Status:** design, scope approved in conversation (national languages + English; residential + commercial)
**Builds on:** `2026-10-03-lease-agreement-documents-slice0-design.md` (document model, clause library, fidelity gate, NL residential reference in 13 languages)

## Problem

Only NL has a real lease document. BE, DE, ES, FR, GB and PT still render the short placeholder lease (LEGACY rows); AT, CA, CH, CZ, DK, FI, IE, IT, LU, NO, PL, SE and US have nothing, and Greece is not even in the rent-regulation catalog. Landlords in those countries get a generic example text or the "not available for this country" state.

## Intended outcome

For every country in the rent-regulation catalog (AT BE CA CH CZ DE DK ES FI FR GB IE IT LU NL NO PL PT SE US) **plus Greece (GR, to be added to the catalog)**, a landlord can generate a **residential long-term** lease and a **commercial** lease in the country's national language(s) and in English, built from the existing clause library with country-specific, law-aware clauses. Success: every (country, kind) in the matrix below has real documents, seeded clause rows, and passes the fidelity/catalog gates; the placeholder lease remains only for kinds not covered (mixed-use, agricultural, short-term, student/mobility).

## Decisions (from the conversation)

- **Languages:** the country's national language(s) that exist among the app's 13 document languages **plus English**; the other app languages fall back to the national language with the existing courtesy/fallback notices. The national-language document is authoritative; English (and any second national language) is a machine-drafted translation checked by the fidelity gate.
- **Kinds:** `RESIDENTIAL` and `COMMERCIAL`. Furnished units reuse RESIDENTIAL via the fallback chain. NL gets a COMMERCIAL lease too (residential already exists in 13 languages).
- **Everything unvetted by counsel**, marked as such in every PDF, as for NL. Each pack lists the statutory facts it could not verify.

## Matrix (authoritative language first; `en` always added)

| Country | Languages | Notes |
|---|---|---|
| AT | de, en | MRG regimes (full/partial application), minimum term rules |
| BE | nl, fr, en | residential law is regional (Flanders/Wallonia/Brussels): region conditionals; upgrades the placeholder |
| CA | en, fr | provincial law (Ontario standard form etc.): province conditionals; Quebec in fr |
| CH | de, fr, it, en | OR art. 253ff; cantonal formalities |
| CZ | en | Czech is not an app language: English only, flagged in the PDF notice |
| DE | de, en | BGB 535ff; upgrades the placeholder |
| DK | da, en | |
| ES | es, en | LAU 29/1994; regional rent regimes; upgrades the placeholder |
| FI | fi, sv, en | |
| FR | fr, en | loi 89-462 and mandatory model lease; upgrades the placeholder |
| GB | en | nations differ (England/Wales/Scotland/NI): nation conditionals; verify the current England regime after the Renters' Rights reforms; upgrades the placeholder |
| GR | el, en | **new catalog country** |
| IE | en | RTA 2004 |
| IT | it, en | L. 431/1998 contract types |
| LU | fr, de, en | written lease with mandatory clauses on pain of nullity |
| NL | nl, en (commercial only) | residential done (13 languages) |
| NO | nb, en | |
| PL | pl, en | |
| PT | pt, en | NRAU; upgrades the placeholder |
| SE | sv, en | |
| US | en | state law: state conditionals (DC etc. per the catalog regions); federal disclosures |

About 84 documents in total (41 residential + 43 commercial).

## Design

### D1 — Shared groundwork (one task before the packs)

1. **Catalog registry for the gates.** `LeaseDocumentCatalogTest`, `LeaseDocumentFidelityTest`, `TranslatedResidentialLeaseRenderTest` and the DB consistency test are NL-specific today. Generalize them over a registry of `(country, kind, authoritativeLanguage, enforcedLanguages)`. NL residential keeps all 13 languages.
2. **Locator national languages table** (`LeaseDocumentLocator`): AT de; BE nl,fr; CA en,fr; CH de,fr,it; CZ none (English fallback); DE de; DK da; ES es; FI fi,sv; FR fr; GB en; GR el; IE en; IT it; LU fr,de; NL nl; NO nb; PL pl; PT pt; SE sv; US en.
3. **Number-word lint per authoritative language.** Today only Dutch is linted for spelled-out numbers (digits convention). Extend `numberWordViolations` with a per-language word list (en, de, fr, es, pt, it, sv, da, nb, fi, el, pl, nl) applied to every authoritative document.
4. **Template variables:** add `regionCode` (contract region, nullable) and `countryCode` to the template context so documents can branch on region/nation/state. Commercial leases print fill-in lines for data the system does not hold (company registration number, VAT number).
5. **Seeds:** one migration per country pack (V093+) inserting the clause rows (`lease_kind` RESIDENTIAL / COMMERCIAL), with identifiers `LCT<CC><RES|COM><nn>` padded to 29 characters and the audit columns set to the system user.
6. **Title/summary bundle keys** for each new clause key in all 13 `document-lease-agreement[_lang].properties` files (parity test). Clause keys are shared across languages of a (country, kind).
7. **Regional rule:** one document per (country, kind, language); where the law differs materially by region, branch with `th:if` on `regionCode`; where the region is unknown the text states the general rule and that regional rules may apply. The fidelity gate compares structure per language, so branches must be identical across languages.

### D2 — Greece in the rent-regulation catalog

Add `GR` to `rent-regulations.json` (catalog audit conventions: see `backend/buurman-backoffice` rent-regulation code and the 2026-09-26 tenancy-rules-reference design) with country-level tenancy facts, sources and legal bases, researched from primary sources; surfaced on the backoffice rent-regulations page like the other countries. The GR lease packs rely on the same research.

### D3 — A country pack (repeat per country)

Each pack: (1) legal research against primary sources, with a sources/verification-date block and an explicit Unverified list; (2) the clause set for RESIDENTIAL and COMMERCIAL (keys, required/optional/pinned, order) chosen from the country's legal requirements (mandatory content, deposit limits, notice rules, indexation, registration duties, disclosures); (3) authoritative documents in the first national language, then the other languages and English; (4) bundle keys, seed migration, registry entry; (5) tests: catalog, fidelity gate, render tests, DB consistency; (6) independent review (statutory fidelity, translation fidelity); fix round; (7) pack report with the Unverified list.

### D4 — Order

0. Groundwork (D1) and Greece catalog entry (D2). 1. Placeholder upgrades: DE, FR, ES, PT, BE, GB. 2. New European countries: AT, DK, FI, GR, IE, IT, LU, NO, PL, SE, CZ. 3. CH, CA, US (regional). 4. NL commercial. 5. Finish: backoffice banner and docs reflect the new coverage.

## Non-goals

- Mixed-use, agricultural, short-term and student/mobility leases for the new countries.
- Languages outside the app's 13 (e.g. Czech, Irish, Catalan).
- Counsel review (tracked per pack as an Unverified list).

## Risks

- Volume (about 84 documents) and legal accuracy: mitigated by the fidelity gate, per-pack independent review, and honest draft marking; not eliminated.
- Federal/regional systems (CA, CH, GB, US, BE) cannot be fully covered by one text: region conditionals plus a general fallback, flagged per pack.
