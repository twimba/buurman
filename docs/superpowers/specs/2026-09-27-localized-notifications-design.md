# Localized notifications — design

BUUR-109, slice A+B. Date: 2026-09-27.

## Intended outcome

A tenant or landlord receives every notification in their own language, on
every channel, and the build fails if any locale is incomplete. Today email is
already fully localized; SMS is not, recipients have no language of their own,
and nothing stops a translation gap from shipping.

Success looks like:

- A tenant whose contact record says `pt` gets the payment reminder in
  Portuguese, by email and by SMS, and the SMS costs exactly one segment.
- `mvn test` and `yarn test` fail on a missing key, an unrenderable
  template × locale pair, or an SMS that spills into a second segment.

## Current state

The ticket's "current state" section is stale. Verified on this branch:

| Ticket claim | Reality |
| --- | --- |
| 24 email templates, English only | 25 files — 24 renderable templates plus the shared `_email-base` fragment — all on `#{...}`, no hardcoded copy |
| bundles not wired into email rendering | `EmailTemplateConfig` wires `emailMessageSource` (`email-subjects`, `email-bodies`) into a dedicated `emailTemplateEngine` |
| no locale resolution | `NotificationServiceImpl.resolveRecipientLocale`: explicit → user pref → team default → `en` |
| translations missing | 13 locales, 203 body + 21 subject keys, **0 key diff** vs `en`, **0** template keys absent from bundles |
| frontend hardcoded English | `NotificationDetailModal` is on `useTranslation('admin')`; `public/locales/<13>/*.json` exist |

So scope item 1 is substantially shipped for email. What is actually open:

1. **SMS ignores locale.** `LocalSmsSender.render` and `TwilioSmsSender.render`
   both accept a `Locale` and drop it. Bodies are hardcoded English strings in
   a 15-case `switch` plus a generic fallback, and the two senders' switch
   blocks are **byte-identical**.
2. **No CI guard.** Nothing fails on a missing key or an unrenderable
   template × locale. An audit run while writing this spec found real drift:
   380 missing backend keys and 16 frontend discrepancies (detail below).
3. **No recipient language.** `preferred_language` exists nowhere — not in the
   schema, the OpenAPI spec, or the frontend.

## Out of scope

Separate specs, in this order: in-app notification center (ticket item 3),
bounce/complaint handling (item 6), backoffice template management (item 4),
communications timeline (item 5). Web push is a follow-up to the inbox.

There is **no tenant portal in this repository**, so the ticket's "settable
from the contact page and tenant portal" can only be half-delivered: the
contact page. Flagged rather than silently dropped.

## S1 — One canonical locale list

The 13 languages are currently written out by hand in three places:
`DocumentLanguages.SUPPORTED`, `I18nConfig.localeResolver`, and the frontend's
`supportedLngs`. A guard that discovers locales is worthless if the list it
checks against can itself drift.

- `DocumentLanguages` gains `ORDERED` (a `List`) alongside `SUPPORTED`. The
  existing field is a `Set.of(...)`, whose iteration order is unspecified —
  parameterized test names and report output must be stable.
- `I18nConfig.localeResolver` derives its supported locales from
  `DocumentLanguages.ORDERED` instead of its own literal list.
- A new OpenAPI `LanguageCode` enum schema, referenced by
  `contract.documentLanguages` (today a bare `type: string` array),
  `teamPreferences.defaultLanguage`, `userPreferences.language`, and the new
  contact field. Orval generates it, so the frontend asserts `supportedLngs`
  against the generated enum and the list cannot drift across stacks again.

## S2 — SMS localisation with a one-segment guarantee

Cost control requires every SMS to be exactly one segment. One segment is 160
characters only in **GSM-7**. `á ã â ó í ú ê ł ą ć ę ś ż` and all lowercase
Greek fall outside GSM-7, forcing UCS-2 at **70** characters — so pt, es, fr,
it, pl and el cannot hold a natural-spelling reminder in one segment. `€` costs
two septets even within GSM-7.

Decision: **fold Latin-script locales to GSM-7, keep Greek native and terse.**

- New bundle family `messages/sms-bodies{,_xx}.properties` — 16 keys (the 15
  named templates plus the generic fallback) × 13 locales. The
  `emailMessageSource` bean is renamed `notificationMessageSource`
  and gains the third basename; `EmailSubjectResolver` and
  `EmailTemplateConfig` follow the rename.
- New `SmsBodyRenderer` in `buurman-notifications` replaces the duplicated
  `getSmsTemplate` switch. `LocalSmsSender` and `TwilioSmsSender` both delegate
  and finally honour the `Locale` they already receive. Removes ~60 duplicated
  lines.
- New `SmsSegment` in `buurman-common` (pure Java, no Spring):
  - `Gsm7.fold(String)` — maps non-GSM-7 characters to their GSM-7 equivalent
    (`á→a`, `ł→l`, `ő→o`), leaving characters it cannot fold untouched.
  - `SmsSegment.encodingOf(String)` — `GSM_7` (160 septets, extension-table
    characters including `€` costing 2) or `UCS_2` (70 code units).
  - `SmsSegment.fitToOneSegment(String body, String truncatableValue)` — folds,
    and if the result still exceeds the budget truncates the longest
    interpolated **value** with an ellipsis. It never truncates the sentence,
    so the message stays grammatical.

The fold/budget decision is locale-agnostic: fold first, then whatever is
*still* UCS-2 (in practice Greek) gets the 70-character budget. No locale
allowlist to maintain. Greek copy is authored terse against that budget,
dropping the property name where it does not fit.

## S3 — Recipient language

`V073__contact_preferred_language.sql`:

```sql
ALTER TABLE contacts ADD COLUMN preferred_language VARCHAR(2);
ALTER TABLE contacts ADD CONSTRAINT chk_contact_preferred_language
  CHECK (preferred_language IS NULL OR preferred_language IN
    ('en','nl','de','fr','pt','es','sv','it','fi','el','pl','da','nb'));
```

`NotificationServiceImpl.resolveRecipientLocale` is extracted into a
`RecipientLocaleResolver` with this chain:

1. `contacts.preferred_language` — when `recipientContactId` is present
2. `user_preferences.language` — when `recipientUserId` is present
3. `request.contextLanguageTag` — contract document language, caller-supplied
4. `team_preferences.default_language`
5. `en`

**Semantic change.** `SendNotificationRequest.languageTag` is documented as
"Overrides user/team preference resolution" and is consulted first. The
ticket's required order puts recipient preference above the contract document
language, so the field is re-ranked to position 3 and renamed
`contextLanguageTag` to stop the old name implying an override. Two callers
pass it — `PaymentReminderService` (contract document language, via
`DocumentLanguages.firstSupportedOrDefault`) and the reminder-preview path
(team default). Both supply context rather than an override, so the re-rank is
behaviour-preserving for them; `PaymentReminderServiceTest:321` asserts only
that the request carries `nl` and is unaffected.

The resend path (`NotificationServiceImpl` ~line 262) carries its **own
duplicated** copy of the resolution — user preference, else team default — and
consults neither the contact nor the original's language. Nothing persists a
language tag on `notifications`, so a resent tenant reminder can come back in a
different language than the original. The extracted resolver serves both paths,
fixing that.

Then the ordinary entity trail: `Contact` POJO, `ContactResponse`, create and
update requests, `ContactRepository` insert/update, MapStruct mapper, OpenAPI
schemas (`make bundle-openapi`), and a language select on the contact form.

## S4 — The generic i18n guard

Backend tests live in `buurman-app/src/test` — its classpath sees every
module's resources, the pattern `TenantReminderToneRenderTest` already uses.
Discovery is via `classpath*:messages/*.properties`, so a new bundle family is
covered the day it is added, with no test to update.

| Test | Asserts |
| --- | --- |
| `I18nBundleParityTest` | discovers every `messages/*.properties` family; all 13 locales present, exact key parity against `en`, and equal `{0}`/`{1}` placeholder arity per key. Excludes `test-*` fixtures. |
| `EmailRenderMatrixTest` | 24 renderable templates × 13 locales render through `emailTemplateEngine` without throwing, produce no unresolved `#{` or `??key??` markers, and yield a non-empty subject. `_`-prefixed fragment files are skipped — they are not standalone templates. |
| `SmsSegmentBudgetTest` | 16 bodies × 13 locales with worst-case fixtures (30-character property name, long contact name) resolve to exactly one segment after fold and truncation. |
| `locales.parity.test.ts` | `public/locales/*/*.json` namespace set and flattened key set match `en`; the directory set equals the generated `LanguageCode`. |

### Plural awareness is mandatory

The frontend uses i18next plural suffixes heavily (`count_one`/`count_other`,
91 and 52 occurrences respectively). CLDR plural categories differ by language:
Polish needs one/few/many/other where English needs one/other. A naive key
comparison therefore produces false positives, and the next person to see a red
build will weaken the test rather than fix it.

`locales.parity.test.ts` strips i18next plural suffixes before comparing base
keys, then separately asserts that each locale supplies exactly the CLDR plural
categories its language requires, via `Intl.PluralRules`.

This immediately surfaces a real English bug: `en/payments.json` has
`selection.selected` interpolating `{{count}}` with no plural forms at all,
while `pt` and `el` correctly supply `selected_other` and `pl` supplies none
despite needing four categories.

## S5 — Drift remediation and translation surface

Roughly 600 new strings.

**380 backend keys, all missing (0 extra anywhere), all jurisdiction legal
clauses:**

| Family | Keys | Locales |
| --- | --- | --- |
| `document-extension` | 20 (`legal.AT` … `legal.US`) | da, el, fi, nb, pl |
| `document-rent-change` | 20 (`rentchange.legal.*`) | da, el, fi, nb, pl |
| `document-deposit-statement` | 8 (`deposit.legal.BE/DE/ES/FR/GB/IT/NL/PT`) | all 12 |
| `document-payment-notice` | 8 (`notice.legal.*`) | all 12 |

**208 SMS strings** — 16 bodies × 13 locales, authored against the
one-segment budget.

**16 frontend entries:**

- `contracts.json` — `countryMetadata.fields.depositSum` missing in all 12
- `properties.json` — `map.mapView`, `map.noStreetView`, `map.streetView`
  missing in `nb`
- `payments.json` — `selection.selected` plural forms, resolved by the CLDR
  rule above rather than by copying keys around

Workflow: machine-translation pass, then `docs/i18n-review.md` carrying a
per-locale native-review checklist, as the ticket specifies.

**Recorded risk.** The 380 clauses are legal wording. A mistranslated clause in
a tenant-facing letter is worse than an English fallback, and these will be
machine-quality until a native legal review happens. An explicit
fallback-permitted policy for `*.legal.*` was offered and declined in favour of
translating them; proceeding as instructed, with the risk noted here.

## Testing

- `SmsSegmentTest` (unit, `buurman-common`) — folding table, GSM-7 extension
  characters counting 2, the 160/70 boundary, truncation picking the longest
  value and preserving the sentence.
- `SmsBodyRendererTest` — locale selection, variable interpolation, and that
  both senders produce identical output for the same input.
- `RecipientLocaleResolverTest` — each rung of the chain in isolation, and that
  contact preference beats a caller-supplied `contextLanguageTag`.
- `ContactRepositoryIntegrationTest` — `preferred_language` round-trips, the
  CHECK constraint rejects an unsupported code, and a query scoped to one
  `team_id` cannot see another team's contacts (closing the isolation gap
  CLAUDE.md calls out).
- The four guard tests of S4.

Acceptance criteria covered by this slice: the Portuguese-tenant reminder and
the Dutch payment-received notice (S3), and "every template renders in all 13
locales in CI with no missing keys" (S4). The unread-badge and bounce-warning
criteria belong to the follow-up specs.
