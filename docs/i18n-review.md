# Translation review

Roughly 600 strings were added machine-translated in BUUR-109 and need a native
speaker's pass. This file tracks that pass. Tick a language only when someone who
speaks it has read every listed bundle end to end.

## What was added

| Surface | Strings | Risk |
| --- | --- | --- |
| `messages/sms-bodies_*.properties` | 192 (16 × 12 non-English) | Low. Short, factual, and the budget test proves each fits one segment. |
| `document-extension`, `document-rent-change`, `document-deposit-statement`, `document-payment-notice` — the `*.legal.<COUNTRY>` keys | 380 | **High. Jurisdiction legal clauses in tenant-facing letters.** |
| `public/locales/*/{contracts,properties,payments,tenants}.json` | ~130 | Low. UI labels, plus the plural forms listed below. |

The UI figure covers `countryMetadata.fields.depositSum` (12 languages), three
`map.*` keys in `nb`, `selection.selected` pluralised in 13, the three
`form.preferredLanguage*` keys in 13, and 58 plural bases that previously had no
resolvable form at some counts.

## The legal clauses need a lawyer, not only a native speaker

`legal.AT` is Austrian tenancy law whatever language it is read in. A translation
must not adapt the legal substance to the reader's country, and must keep statute
names and article numbers intact. Until a review is ticked below, these clauses are
machine-quality.

An explicit fallback-permitted policy — letting `*.legal.*` resolve to the English
clause rather than translating it — was considered and rejected in favour of
translating them. See `docs/superpowers/specs/2026-09-27-localized-notifications-design.md`.

## Sign-off

| Language | SMS bodies | UI strings | Legal clauses | Reviewer | Date |
| --- | --- | --- | --- | --- | --- |
| nl | [ ] | [ ] | [ ] | | |
| de | [ ] | [ ] | [ ] | | |
| fr | [ ] | [ ] | [ ] | | |
| pt | [ ] | [ ] | [ ] | | |
| es | [ ] | [ ] | [ ] | | |
| sv | [ ] | [ ] | [ ] | | |
| it | [ ] | [ ] | [ ] | | |
| fi | [ ] | [ ] | [ ] | | |
| el | [ ] | [ ] | [ ] | | |
| pl | [ ] | [ ] | [ ] | | |
| da | [ ] | [ ] | [ ] | | |
| nb | [ ] | [ ] | [ ] | | |

## What a reviewer checks

- The copy reads as something a professional landlord would send, not as a translation.
- Every `{placeholder}` and `{{count}}` token is intact and in a position that makes
  the sentence grammatical once a real value lands in it.
- Greek SMS copy is terse on purpose — its script costs 70 units per segment rather
  than 160. Do not "restore" the detail the English version carries. `sms.body.welcome`
  and `sms.body.password-changed` in `el` are deliberately shorter than the other twelve
  languages for the same reason, and `sms.body.contract-created` deliberately drops
  `{propertyName}`, which the parity guard permits only for `sms-bodies`.
- Plural forms cover the categories the language actually needs. `yarn test` enforces
  this; if it complains about a language you are reviewing, it is right.

## Open backlog: keys that interpolate a count without plural forms

50 keys interpolate `{{count}}` but carry no plural forms in any language. Some are
correct as they stand — `"Your Teams ({{count}})"` and `"+{{count}} more"` are
parenthetical tallies that never take grammatical agreement, and this codebase also
uses an explicit `*Plural` key convention in places. Others plainly do need agreement,
for example `"{{count}} properties found, but none have financial data yet."`,
`"{{count}} valid"` / `"saved"` / `"failed"`, `"{{count}} files"`,
`"across {{count}} categories"` and `"Preview (first {{count}} rows)"`.

Telling those apart is a per-key judgement in each language, so it was deliberately
left out of the automated guard rather than encoded as a rule someone would later
silence. A reviewer working through their language should decide, key by key, which
need plural forms and add them; the parity guard will then hold whatever is added.

Reproduce the current list with:

```bash
cd frontend/app/public/locales && node -e '
const fs=require("fs");
const SUF=/_(zero|one|two|few|many|other)$/, strip=k=>k.replace(SUF,"");
const flat=(o,p="")=>Object.entries(o).flatMap(([k,v])=>
  v&&typeof v==="object"&&!Array.isArray(v)?flat(v,p+k+"."):[p+k]);
for(const f of fs.readdirSync("en").filter(x=>x.endsWith(".json")).sort()){
  const o=JSON.parse(fs.readFileSync(`en/${f}`,"utf8")), keys=flat(o);
  for(const k of keys){
    const v=k.split(".").reduce((a,p)=>a&&a[p],o);
    if(typeof v!=="string"||!v.includes("{{count}}"))continue;
    if(keys.some(x=>SUF.test(x)&&strip(x)===strip(k)))continue;
    console.log(`${f} ${k}: ${JSON.stringify(v)}`);
  }}'
```
