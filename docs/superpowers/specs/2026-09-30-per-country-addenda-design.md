# Per-country addenda/letters — design

BUUR-105 item 4. Date: 2026-09-30.

## Intended outcome

A DE landlord's rent-increase letter carries the §558 Mieterhöhung
structured justification a real German rent-increase notice needs; an NL
landlord's cites the actual legal basis. Today every country gets the same
generic letter with one optional paragraph bolted on.

Success looks like: generating a rent-increase letter for a DE contract
produces a letter with a distinct, structured "legal basis" section (index/
comparison-method justification, statutory cap reference) instead of the
same one-paragraph `legal.DE` block every other country also gets a version
of.

## Legal content — explicit scope and disclaimer

Same posture as the termination-workflow spec: this feature is a
**mechanism** for composing multiple, ordered, country-specific clauses
per letter — it is not, by itself, a source of verified legal text. The
specific NL/DE example content seeded here follows directly from what the
ticket itself names ("NL rent-increase letter must cite the legal basis";
"DE Mieterhöhung §558 form requirements") using existing message-bundle
conventions already in production for the current single-clause text — it
extends existing content, it doesn't research new jurisdictions.

## Current state (evidence)

Every letter template (`rent-increase-letter`, `extension-addendum`,
`rent-change`, `payment-formal-notice`, `deposit-statement`) has exactly
one `th:if="${legalClause != null}"` block, fed by
`LetterExporterHelper.legalVariables()` → `resolveLegalClause()`, which
resolves a **single** message key `{keyPrefix}{countryCode}` (e.g.
`legal.NL`) and returns one optional string. `LetterTemplateService`'s own
class Javadoc states the current design intent explicitly: "country-specific
sections are handled within templates via `th:if` conditionals on
`countryCode`" — this spec changes that intent, not just adds data to it.

Content today lives in `.properties` bundles
(`document-{extension,rent-change,payment-notice}*.properties`), one
`legal.{COUNTRY}` key per country per document type, already translated
across the existing 13 supported languages. This spec extends that same
bundle mechanism — no new content-storage system.

## Key decision: multiple ordered clauses, not one paragraph

`legalVariables()` changes from returning `Optional<String>` to
`List<LetterClause>` (`record LetterClause(String titleKey, String bodyKey)` —
title is new, since a §558-style structured section benefits from a
heading the current single paragraph never needed). A country/document-type
combination with no entry in the new lookup falls back to exactly today's
behavior wrapped as a single-item list (`legal.{COUNTRY}` unchanged) — so
every existing country/template combination that isn't explicitly extended
renders identically to before this change. This is the smallest change
that gets the ticket's two named examples (NL, DE rent-increase) without
touching the other four document types' existing single-clause behavior
unless they're deliberately extended later.

## S1 — Config (no schema change)

`CountryLetterClauseCatalog` (`buurman-letters`, a small static config
class — not a DB table, since this is developer-maintained structural
config, not landlord- or backoffice-editable content, unlike the lease
clause library which genuinely needs runtime editability):

```java
public record LetterClauseKey(String titleKey, String bodyKey) {}

public final class CountryLetterClauseCatalog {
  // documentType -> countryCode -> ordered clause keys.
  // Absent entries fall back to the single legacy `legal.{COUNTRY}` clause.
  private static final Map<String, Map<String, List<LetterClauseKey>>> CLAUSES =
      Map.of(
          "rent-increase-letter", Map.of(
              "NL", List.of(new LetterClauseKey("legal.NL.basis.title", "legal.NL.basis.body")),
              "DE", List.of(
                  new LetterClauseKey("legal.DE.section558.title", "legal.DE.section558.body"),
                  new LetterClauseKey("legal.DE.comparisonMethod.title", "legal.DE.comparisonMethod.body"))));

  public static List<LetterClauseKey> resolve(String documentType, String countryCode) { ... }
}
```

A static config class, not a DB table, because: it changes with template
structure (a code change to `generic.html`'s clause-rendering loop already
requires a deploy), and letting non-developers edit *which* legal sections
a document type includes (as opposed to editing translated text, which
already happens in `.properties` files without a deploy) is a bigger
responsibility than this ticket asks for. If per-country clause structure
needs runtime editability later, the lease-agreement clause library's
DB-backed pattern is right there to extend to letters too — deliberately
not done now (YAGNI: no stated requirement for backoffice-editable letter
clause structure, only for content, which `.properties` already gives).

## S2 — Backend

`LetterExporterHelper.legalVariables()` signature changes from returning
`Optional<String> legalClause` to `List<Map<String,String>> legalClauses`
(each entry `{title, body}`, pre-resolved via `MessageSource` at the
document's locale — templates get plain resolved strings, never raw keys,
matching how every other variable already reaches Thymeleaf). Internally:
`CountryLetterClauseCatalog.resolve(documentType, countryCode)`, and if
empty, falls back to the existing single-`legal.{COUNTRY}`-key resolution
wrapped as a one-item list.

Every exporter that calls `legalVariables()` (`RentIncreaseLetterExporter`,
`ContractExtensionAddendumExporter`, `RentChangeDocumentExporter`,
`PaymentFormalNoticeExporter`) passes its own `documentType` string through
unchanged — this is a signature widening, not a new call pattern per
exporter.

## S3 — Templates

Every `generic.html`'s single `th:if="${legalClause}"` block becomes a
`th:each="clause : ${legalClauses}"` loop rendering `clause.title` (new — a
heading, styled consistently with existing section headings already in
these templates) then `clause.body`. For a country with exactly one legacy
clause and no title (the fallback path), the template needs to handle a
missing/empty title gracefully (render the body-only paragraph exactly as
today) — confirmed as the correct default in the implementation plan by
checking whether the fallback wrapper supplies an empty title or omits the
field entirely.

## S4 — Seed content

New message keys in `document-rent-change.properties` (+ translations) and
`document-extension.properties`:
- `legal.NL.basis.title`/`.body` (rent-increase letter) — the legal-basis
  citation the ticket names, replacing the existing bare `legal.NL` for
  this document type only (other document types' `legal.NL` untouched).
- `legal.DE.section558.title`/`.body` + `legal.DE.comparisonMethod.title`/`.body`
  (rent-increase letter) — the §558 structured justification the ticket
  names, as two ordered sections rather than one paragraph.

`legal.NL`/`legal.DE` for the other four document types (extension
addendum, rent-change, formal notice, deposit statement) are **untouched**
— they keep rendering via the fallback single-clause path exactly as
before this change, since the ticket only names rent-increase-letter
examples.

## Testing

- `CountryLetterClauseCatalogTest` — DE/rent-increase-letter resolves two
  ordered clauses; NL/rent-increase-letter resolves one; an
  unconfigured combination (e.g. FR/rent-increase-letter) returns empty,
  triggering the fallback.
- `RentIncreaseLetterExporterTest` — DE-country contract's rendered HTML
  contains both §558 section headings in order; an unconfigured country's
  output is byte-for-byte identical to this change's `git diff` base (proves
  the fallback path is truly behavior-preserving, not just "returns
  something").
- Existing extension-addendum/rent-change/formal-notice/deposit-statement
  exporter tests continue passing unmodified — proves the four untouched
  document types are genuinely unaffected.

## Out of scope

- Backoffice-editable letter clause structure (see S1 — deliberately a
  static config, not a DB table, for this ticket).
- Any document type beyond rent-increase-letter getting new multi-clause
  content (the plumbing supports it; no content is seeded for the other
  four beyond what already exists).
- Countries beyond NL/DE getting new structured content (the ticket names
  only these two with concrete examples).
