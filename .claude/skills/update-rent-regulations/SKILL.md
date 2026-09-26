---
name: update-rent-regulations
description: Review and update Buurman's bundled rent-regulation dataset (rent-regulations.json) against authoritative government sources — correct past mistakes, add newly-published regulations and index values, and report a summary of every change. Use when the user wants to audit, refresh, fact-check, or bring the rent regulations up to date.
argument-hint: [country code(s) e.g. "NL FR", "all", or a year — empty = ask]
disable-model-invocation: true
allowed-tools: Read, Edit, Write, Grep, Glob, Bash, WebSearch, WebFetch, Agent, Workflow
---

# Update Rent Regulations

Audit and refresh the canonical rent-regulation dataset against **authoritative government / official statistical sources**, then update the bundled file so a backoffice admin can reload it in the app.

**The asset** (single source of truth):
`backend/buurman-backoffice/src/main/resources/rent-regulations/rent-regulations.json`

You only edit that JSON file. You do **not** touch the database, run the reload, or commit — the buurmie reloads manually via the backoffice **Reload from catalog** button. Treat the data as a valuable, curated Buurman asset: accuracy beats coverage.

## Goals (this run)

a. **Review existing data** for mistakes (wrong %, wrong index value, wrong effective date, stale "current-year" rows, misattributed regime/type).
b. **Find new data** — regulations or index values published since `generatedAt` (new annual caps, new index releases, framework changes, new regions/zones).
c. **Prefer authoritative sources.** Use the official body, not a news summary or aggregator. Many rows already carry a `sourceUrl` to the right body — start there; if missing, find the official source.
d. **Update the JSON** with corrections and additions (and remove anything proven wrong/superseded), keeping the schema valid.
e. **Record tenancy facts that are not rent-increase caps** — notice periods, minimum/maximum tenancy duration, deposit rules, lease-form formalities, registration duties, fixed-amount fees and penalties — in `tenancyRules` (see Step 3) rather than discarding them because they don't fit a `CatalogRule` row.
f. **Report a summary** of everything added / updated / deleted / flagged.

## Step 1 — Scope

`$ARGUMENTS` sets scope:
- Country code(s) (`NL`, `FR DE ES`) → only those.
- `all` → all 20 countries (use the **Workflow** option below — 20 countries of web research is large).
- A year (`2026`) → focus on that year's rows across all countries.
- **Empty → ask** the user what to scope. Suggest a sensible default: the current year's rows plus any country whose `lastReviewedAt` is oldest.

Read the file first; never assume its contents from memory. Note its current `version` and `generatedAt` — anything published after `generatedAt` is candidate new data.

## Step 2 — Research (authoritative-first)

For each in-scope country/rule:

1. **Start from the existing `sourceUrl`.** WebFetch it; confirm the current value still matches, and look for a newer release on the same site.
2. **Go to the official body** (table below). Verify the latest published figure: the index value, the cap %, the effective date, the legal basis.
3. **Cross-check** a value with a second official/quasi-official source before changing it. For an index series (IRL, ISTAT FOI, IRAV, KPI…), confirm the exact period label (e.g. "IRL T1 2026") and the number.
4. **Reject** non-authoritative sources for the *value itself* (news sites, landlord blogs, Wikipedia). They may guide you to the official source, but the figure and `sourceUrl` must come from the official body.
5. If uncertain after honest effort, **do not invent** — flag it in the summary as "needs manual confirmation" and leave the existing row unchanged.

Adversarially double-check any change that loosens or tightens a cap by a large margin, or changes a regime — those are the ones most likely to be wrong.

### Authoritative sources already in the dataset (start here)

| Country | Official body / index |
|---|---|
| NL | rijksoverheid.nl, huurcommissie.nl — CBS huurverhoging, CPI |
| DE | gesetze-im-internet.de (BGB), Land Mietspiegel sites — Mietspiegel, Kappungsgrenze §558, §559 |
| FR | insee.fr (IRL), legifrance.gouv.fr, DRIHL/préfecture (encadrement zones) |
| ES | ine.es (IRAV/IPC), boe.es, agenciahabitatge.gencat.cat (Catalonia) |
| IT | istat.it (FOI), confedilizia.it |
| BE | statbel.fgov.be (gezondheidsindex), be.brussels, wallex.wallonie.be |
| PT | ine.pt (coefficient), portaldahabitacao.pt |
| AT | statistik.at (VPI/Richtwert), ris.bka.gv.at |
| LU | statistiques.public.lu (STATEC), logement.public.lu |
| GB | legislation.gov.uk, gov.wales, communities-ni.gov.uk, spice-spotlight.scot |
| IE | gov.ie (RPZ / HICP cap) |
| CH | bwo.admin.ch (Referenzzinssatz), cantonal sites (GE/VD/BS/BL) |
| SE | hyresgastforeningen.se (negotiated) |
| NO | ssb.no (KPI) |
| DK | dst.dk (NPI), retsinformation.dk |
| FI | stat.fi, ara.fi |
| PL | isap.sejm.gov.pl, GUS (wskaźnik przeliczeniowy) |
| CZ | zakonyprolidi.cz, ČSÚ |
| US | per-jurisdiction official sites (rhc.dc.gov, stpaul.gov, portlandmaine.gov, county sites) + local CPI-U |
| CA | provincial sites (alberta.ca, gov.mb.ca, gnb.ca, gov.nl.ca …) — provincial annual guidelines |

When the official source for an in-scope row isn't listed or in the data, find it and record it in `sourceUrl`.

## Step 3 — Update the JSON (keep the schema valid)

Edit `rent-regulations.json` in place. The shape (see `CatalogRule` / `CatalogCountry` in `backend/buurman-common/.../domain/regulation/`):

- Countries ordered by `countryCode`; each has `regions[]` (optional) and `rules[]`.
- **Rule fields** (camelCase, nulls omitted): `regionCode` (null/absent = national; must match a declared region of that country), `year`, `propertyCategory`, `maxIncreasePercentage` (number), `maxIncreaseType` (enum, see below), `indexName`, `indexValue` (number), `effectiveDate` (`YYYY-MM-DD`), `noticePeriodDays`, `frequency` (enum), `additionalConditions`, `sourceUrl`, `notes`, and the dimensional fields `regime`, `propertyType`, `contractType`, `taxRegime`, `tenancyPhase`, `buildYearMin/Max`, `epcClassMin/Max`, `contractSignedAfter/Before`, `landlordMinProperties`, `areaCode`. Dimensional `null` = wildcard ("applies to all").
- Put the official link in `sourceUrl` for every row you change/add. Keep `notes` concise and factual (period, legal basis, what changed).
- **Do not** add `identifier`/`id`/audit fields — those are DB-generated on reload.

**Enum constraint (important):** `maxIncreaseType` must be a constant of `com.buurman.domain.MaxIncreaseType` and `frequency` of `RentFrequency`. If a new regime genuinely needs a new `maxIncreaseType`, you must also add it to:
1. `backend/buurman-common/.../domain/MaxIncreaseType.java`
2. `openapi/src/app.yaml` and `openapi/backoffice.yaml` (the `MaxIncreaseType` enum), then `make bundle-openapi`

Otherwise the value fails to load on reload (this exact gap once shipped a broken `CEILING_RENT`). Prefer an existing constant; only extend the enum when nothing fits, and call it out in the summary.

### `tenancyRules` — display-only tenancy-law facts

Alongside `rules[]`, each `CatalogCountry` carries an optional `tenancyRules[]` array for tenancy-law
facts that are **not** rent-increase caps: notice periods, minimum/maximum tenancy duration, deposit
rules, lease-form formalities, registration duties, fixed-amount fees and penalties. These render as
reference material on the regulations page; nothing computes off them.

**Fields** (camelCase, nulls omitted): `topic`, `regionCode` (omit for a national rule; when present
it **must match a declared region of that country** — same rule as `CatalogRule.regionCode`), `label`,
`value` (free text — the facts are heterogeneous, e.g. "5 years", "DKK 344", "2 months' rent"),
`effectiveFrom` (`YYYY-MM-DD`), `legalBasis`, `sourceUrl`, `notes`.

**`topic` must be one of exactly these constants** of `com.buurman.domain.TenancyRuleTopic` — an
unknown value fails the catalog parse:
`NOTICE_PERIOD`, `TENANCY_DURATION`, `DEPOSIT`, `LEASE_FORM`, `REGISTRATION`, `FEES_AND_PENALTIES`, `OTHER`.

**Never put a rent-increase cap or index value in `tenancyRules`** — those belong in `rules[]` as a
`CatalogRule`. `tenancyRules` is display-only reference material, not a computation input.

**No-history rule:** unlike `rules[]` (which keeps historical rows per year), `tenancyRules` stores
only the CURRENT fact. When a tenancy rule is superseded by a newer one, **replace the entry in
place** — do not keep the old one alongside it — and set `effectiveFrom` to date the fact that is now
current, not the date you happened to update the JSON.

As with `rules[]`, never invent a `value`, `effectiveFrom` or `legalBasis` for a tenancy fact you
could not confirm against an authoritative source this run; leave it out and flag it instead.

After editing, **bump metadata**: set `version` (e.g. `2026.1` → `2026.2`) and `generatedAt` to today's date (`date +%F`).

## Step 4 — Validate

Run the integrity guard (parses the file, checks enum values + region references):

```
cd backend && mvn -q -pl buurman-backoffice -am test -Dtest=RentRegulationCatalogTest -Pquick
```

Also sanity-check JSON validity (`python3 -m json.tool <file> >/dev/null`) and that no `regionCode` references a region not declared in its country. Fix anything that fails before reporting.

## Step 5 — Summary (always end with this)

Report what changed, grouped by country, in this shape:

```
Rent-regulation update — v<old> → v<new>  (<date>)
Scope: <countries/year>

➕ Added (<n>)
  FR · 2026 · PARIS encadrement — loyer de référence majoré 27.4 €/m²  [insee/DRIHL]
🔄 Updated (<n>)
  NL · 2026 · regulated cap 4.10% → 4.50%  (was wrong; CBS confirms 4.50%)  [rijksoverheid.nl]
  IT · 2026 · ISTAT FOI 1.13 → 1.20 (75% = 0.90)  [istat.it]
🗑️ Deleted (<n>)
  ES · 2025 · duplicate IRAV row superseded by Catalonia split  [ine.es]
⚠️ Flagged — needs manual confirmation (<n>)
  CZ · 2026 · no official 2026 figure published yet; left 2025 value

Totals: countries reviewed N · rules added A · updated U · deleted D · flagged F
Validation: RentRegulationCatalogTest ✅   |   version bumped to v<new>
Next: a backoffice admin reloads via "Reload from catalog" to apply.
```

For every Added/Updated/Deleted entry, cite the authoritative source. Be honest about what you could **not** verify — flagged-and-unchanged is the correct outcome when official data isn't available, never a guess.

## Scaling to many countries — Workflow (opt-in)

`all` (or many countries) means ~20 independent research tasks. That's a good fit for a multi-agent **Workflow**: one researcher per country (each verifies against its official body and returns proposed changes with sources), then a synthesis step that applies the vetted changes to the JSON and produces the summary. Only run the Workflow if the user opts into multi-agent orchestration; otherwise process countries sequentially with `WebSearch`/`WebFetch`, or research a few in parallel with `Agent`. Either way, the rules above (authoritative-only, no guessing, cite sources, validate, summarize) are unchanged.
