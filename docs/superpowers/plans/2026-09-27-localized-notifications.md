# Localized Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every notification reaches its recipient in that recipient's own language on every channel, each SMS costs exactly one segment, and the build fails if any locale is incomplete.

**Architecture:** SMS copy moves out of duplicated Java `switch` blocks into a locale-aware message bundle rendered through a shared `SmsBodyRenderer`, with a pure-Java `SmsSegment` utility that folds Latin scripts to GSM-7 and truncates the longest interpolated value to hold one segment. Recipient language becomes a first-class `preferred_language` column on contacts, consumed by a `RecipientLocaleResolver` extracted from the two places that duplicate locale resolution today. A discovery-based parity guard over every message bundle and locale namespace makes translation gaps a build failure.

**Tech Stack:** Java 25, Spring Boot 4.0.2, JOOQ 3.20, Flyway, MapStruct, Thymeleaf, JUnit 5 + AssertJ + Testcontainers; React 19, TypeScript, Vitest, i18next.

**Spec:** `docs/superpowers/specs/2026-09-27-localized-notifications-design.md`

## Global Constraints

- The supported languages are exactly, in this order: `en, nl, de, fr, pt, es, sv, it, fi, el, pl, da, nb`. Thirteen. Never hardcode this list in new code — read it from `DocumentLanguages.ORDERED` (backend) or `SUPPORTED_LANGUAGES` (frontend).
- **MANDATORY:** every `if`, `else`, `for`, `while` body uses curly braces. No brace-less single-statement bodies, ever.
- **MANDATORY:** idiomatic `Optional` API (`map`, `orElse`, `orElseThrow`, `ifPresent`, `flatMap`). Never `if (opt != null)` and never `opt.get()` without an `isPresent()` check.
- Google Java Style (backend), Airbnb (frontend). `mvn package -Pquick` skips formatting; a normal `mvn install` applies it.
- Conventional commits: `feat:`, `fix:`, `docs:`, `chore:`.
- Never modify an existing Flyway migration. The next free version is **V073**.
- Never bypass `team_id` filtering in a repository query.
- Never expose an internal UUID in an API; expose `identifier` (Sid).
- An SMS must occupy exactly one segment: 160 septets in GSM-7, 70 UTF-16 code units in UCS-2.
- The truncation marker is ASCII `"..."`, never `…` (U+2026 is outside GSM-7 and would flip the whole message to UCS-2, halving the budget).
- After editing anything under `openapi/src/`, run `make bundle-openapi`. After that, `cd frontend && yarn generate:api` — the generated clients are gitignored.
- The repo is prettier-clean; run `cd frontend && yarn lint --fix` before committing frontend changes.
- This worktree has unrelated pre-existing modifications to `docker/traefik/dynamic/local-dev.yml` and `keycloak/*.json` from the dev environment. **Never `git add -A` or `git commit -a`.** Stage only the files a step names.

## Review Focus

1. **An interpolated value flips the encoding.** A tenant named `Łukasz` or a property called `Παλαιό Φάληρο` lands in an otherwise-GSM-7 English SMS and silently forces UCS-2, halving the budget to 70 and costing two segments. The budget must be computed on the **final interpolated string**, never on the template. → Task 2, Task 4.
2. **Truncation splits a grapheme.** Cutting a Greek string or an emoji at a fixed character count can leave a lone surrogate or an orphaned combining mark, delivering mojibake. Cuts must land on grapheme boundaries. → Task 2.
3. **A template variable is missing.** Today an unsupplied `{propertyName}` is delivered verbatim to the tenant. A reasonable person expects never to receive a raw `{placeholder}`. → Task 3.
4. **The contact's language is NULL, or the contact belongs to another team.** The resolver must fall through to the next rung — never throw, never return a `null` locale, never read across a `team_id` boundary. → Task 9.
5. **A message key is missing at runtime.** The notification `MessageSource` sets `useCodeAsDefaultMessage(true)`, so a gap renders the literal key (`email.body.welcome.greeting`) into a delivered email rather than falling back to English. The parity guard must therefore be proven non-vacuous — a discovery bug that finds zero bundles would otherwise pass silently. → Task 6.

---

### Task 1: Canonical locale list

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/DocumentLanguages.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/config/I18nConfig.java:30-46`
- Create: `backend/buurman-common/src/test/java/com/buurman/util/DocumentLanguagesTest.java`
- Create: `backend/buurman-core/src/test/java/com/buurman/config/I18nConfigTest.java`
- Create: `frontend/app/src/config/languages.ts`
- Modify: `frontend/app/src/i18n/index.ts:12-26`
- Modify: `openapi/src/app.yaml`

**Interfaces:**
- Consumes: nothing.
- Produces: `DocumentLanguages.ORDERED` (`List<String>`, 13 entries, stable order) and `DocumentLanguages.LOCALES` (`List<Locale>`, same order). Frontend `SUPPORTED_LANGUAGES` (`readonly string[]`) exported from `src/config/languages.ts`. OpenAPI schema `LanguageCode`.

- [ ] **Step 1: Write the failing test for the ordered list**

Create `backend/buurman-common/src/test/java/com/buurman/util/DocumentLanguagesTest.java`:

```java
package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentLanguages")
class DocumentLanguagesTest {

  @Test
  @DisplayName("ORDERED lists the thirteen supported languages in a stable order")
  void orderedIsStable() {
    assertThat(DocumentLanguages.ORDERED)
        .containsExactly("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");
  }

  @Test
  @DisplayName("ORDERED and SUPPORTED describe the same set")
  void orderedAndSupportedAgree() {
    assertThat(DocumentLanguages.ORDERED).containsExactlyInAnyOrderElementsOf(DocumentLanguages.SUPPORTED);
    assertThat(DocumentLanguages.ORDERED).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("LOCALES mirrors ORDERED position for position")
  void localesMirrorOrdered() {
    assertThat(DocumentLanguages.LOCALES).hasSameSizeAs(DocumentLanguages.ORDERED);
    for (int i = 0; i < DocumentLanguages.ORDERED.size(); i++) {
      assertThat(DocumentLanguages.LOCALES.get(i))
          .isEqualTo(Locale.forLanguageTag(DocumentLanguages.ORDERED.get(i)));
    }
  }

  @Test
  @DisplayName("English is the default and is supported")
  void englishIsDefault() {
    assertThat(DocumentLanguages.DEFAULT).isEqualTo("en");
    assertThat(DocumentLanguages.isSupported(DocumentLanguages.DEFAULT)).isTrue();
  }
}
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-common -Dtest=DocumentLanguagesTest`
Expected: FAIL — `cannot find symbol: variable ORDERED`.

- [ ] **Step 3: Add ORDERED and LOCALES**

In `DocumentLanguages.java`, add `java.util.Locale` to the imports and replace the `SUPPORTED` declaration with:

```java
  /**
   * The supported languages in a stable, human-meaningful order. {@code SUPPORTED} is a {@code Set}
   * whose iteration order is unspecified, which makes parameterized test names and report output
   * shuffle between runs. Anything that iterates the languages should use this.
   */
  public static final List<String> ORDERED =
      List.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  public static final Set<String> SUPPORTED = Set.copyOf(ORDERED);

  public static final List<Locale> LOCALES =
      ORDERED.stream().map(Locale::forLanguageTag).toList();
```

- [ ] **Step 4: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-common -Dtest=DocumentLanguagesTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Write the failing test for the locale resolver**

Create `backend/buurman-core/src/test/java/com/buurman/config/I18nConfigTest.java`:

```java
package com.buurman.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.buurman.util.DocumentLanguages;

@DisplayName("I18nConfig")
class I18nConfigTest {

  @Test
  @DisplayName("the locale resolver supports exactly the canonical languages")
  void resolverMatchesCanonicalList() {
    AcceptHeaderLocaleResolver resolver = (AcceptHeaderLocaleResolver) new I18nConfig().localeResolver();

    assertThat(resolver.getSupportedLocales())
        .containsExactlyElementsOf(DocumentLanguages.LOCALES);
    assertThat(resolver.getDefaultLocale()).isEqualTo(Locale.ENGLISH);
  }
}
```

- [ ] **Step 6: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest=I18nConfigTest`
Expected: FAIL — the hand-written list orders locales differently from `DocumentLanguages.LOCALES`.

- [ ] **Step 7: Derive the resolver from the canonical list**

In `I18nConfig.java`, replace the whole `localeResolver` body and drop the now-unused `java.util.List` import if nothing else uses it. Add `import com.buurman.util.DocumentLanguages;`:

```java
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    resolver.setSupportedLocales(DocumentLanguages.LOCALES);
    return resolver;
  }
```

- [ ] **Step 8: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-core -Dtest=I18nConfigTest`
Expected: PASS.

- [ ] **Step 9: Add the LanguageCode schema to OpenAPI**

In `openapi/src/app.yaml`, add this under `components: schemas:` (alphabetical position is not enforced; put it next to the other small enums):

```yaml
    LanguageCode:
      type: string
      description: ISO 639-1 code of a language the product is translated into
      enum: [en, nl, de, fr, pt, es, sv, it, fi, el, pl, da, nb]
```

Then point the existing language-carrying fields at it. In `ContractRequest` and `ContractResponse`, replace both occurrences of:

```yaml
        documentLanguages:
          type: array
          items:
            type: string
```

with:

```yaml
        documentLanguages:
          type: array
          items:
            $ref: '#/components/schemas/LanguageCode'
```

keeping each one's existing `description` and `example` lines beneath it.

- [ ] **Step 10: Bundle and regenerate**

Run: `make bundle-openapi && cd frontend && yarn generate:api`
Expected: `openapi/app.yaml` is regenerated and contains `LanguageCode`; generation completes without error.

- [ ] **Step 11: Create the frontend canonical list**

Create `frontend/app/src/config/languages.ts`. It deliberately does **not** live under
`src/i18n/`: `vitest.config.ts` aliases `@/i18n` to a test mock, and Vite string aliases match by
prefix, so an `@/i18n/languages` import would resolve into that mock inside any component test.

```ts
/**
 * The languages the product is translated into, in the same order as the backend's
 * DocumentLanguages.ORDERED. This is the frontend's single source of truth: i18next's
 * supportedLngs and the locale-parity test both read it, so the list cannot drift.
 */
export const SUPPORTED_LANGUAGES = [
  'en',
  'nl',
  'de',
  'fr',
  'pt',
  'es',
  'sv',
  'it',
  'fi',
  'el',
  'pl',
  'da',
  'nb',
] as const;

export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];
```

- [ ] **Step 12: Point i18next at it**

In `frontend/app/src/i18n/index.ts`, add `import { SUPPORTED_LANGUAGES } from '../config/languages';` and replace lines 12-26 (the inline `supportedLngs` array) with:

```ts
    supportedLngs: [...SUPPORTED_LANGUAGES],
```

- [ ] **Step 13: Verify the frontend still builds**

Run: `cd frontend && yarn lint --fix && yarn build`
Expected: no lint errors, build succeeds.

- [ ] **Step 14: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/util/DocumentLanguages.java \
        backend/buurman-common/src/test/java/com/buurman/util/DocumentLanguagesTest.java \
        backend/buurman-core/src/main/java/com/buurman/config/I18nConfig.java \
        backend/buurman-core/src/test/java/com/buurman/config/I18nConfigTest.java \
        frontend/app/src/config/languages.ts frontend/app/src/i18n/index.ts \
        openapi/src/app.yaml openapi/app.yaml
git commit -m "refactor(i18n): make DocumentLanguages the canonical locale list"
```

---

### Task 2: SmsSegment — GSM-7 folding and the one-segment budget

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/util/SmsSegment.java`
- Create: `backend/buurman-common/src/test/java/com/buurman/util/SmsSegmentTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `SmsSegment.Encoding` (`GSM_7`, `UCS_2`); `static String fold(String)`; `static boolean isGsm7(String)`; `static Encoding encodingOf(String)`; `static int unitsOf(String)`; `static int singleSegmentBudget(String)`; `static boolean fitsOneSegment(String)`; `static String fitToOneSegment(String body, String truncatableValue)`.

- [ ] **Step 1: Write the failing test**

Create `backend/buurman-common/src/test/java/com/buurman/util/SmsSegmentTest.java`:

```java
package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SmsSegment")
class SmsSegmentTest {

  @Nested
  @DisplayName("fold")
  class Fold {

    @Test
    @DisplayName("leaves characters GSM-7 already covers untouched")
    void keepsNativeGsm7Characters() {
      assertThat(SmsSegment.fold("Café Müller à Ørsted ñ É ß")).isEqualTo("Café Müller à Ørsted ñ É ß");
    }

    @Test
    @DisplayName("strips diacritics GSM-7 does not cover")
    void stripsUncoveredDiacritics() {
      assertThat(SmsSegment.fold("Pagamento está atrasado")).isEqualTo("Pagamento esta atrasado");
      assertThat(SmsSegment.fold("José Antonio García")).isEqualTo("Jose Antonio Garcia");
    }

    @Test
    @DisplayName("folds letters that carry no decomposable diacritic")
    void foldsNonDecomposableLetters() {
      assertThat(SmsSegment.fold("Łukasz Wałęsa")).isEqualTo("Lukasz Walesa");
      assertThat(SmsSegment.fold("œuvre")).isEqualTo("oeuvre");
    }

    @Test
    @DisplayName("leaves Greek alone — there is no acceptable Latin fold for it")
    void leavesGreekAlone() {
      assertThat(SmsSegment.fold("Καθυστερημένη πληρωμή")).isEqualTo("Καθυστερημένη πληρωμή");
    }
  }

  @Nested
  @DisplayName("budget")
  class Budget {

    @Test
    @DisplayName("GSM-7 text gets 160 septets")
    void gsm7Budget() {
      String body = "a".repeat(160);
      assertThat(SmsSegment.encodingOf(body)).isEqualTo(SmsSegment.Encoding.GSM_7);
      assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
      assertThat(SmsSegment.fitsOneSegment(body + "a")).isFalse();
    }

    @Test
    @DisplayName("extension-table characters cost two septets each")
    void extensionCharactersCostTwo() {
      assertThat(SmsSegment.unitsOf("€")).isEqualTo(2);
      assertThat(SmsSegment.unitsOf("[]")).isEqualTo(4);
      assertThat(SmsSegment.fitsOneSegment("€".repeat(80))).isTrue();
      assertThat(SmsSegment.fitsOneSegment("€".repeat(81))).isFalse();
    }

    @Test
    @DisplayName("any non-GSM-7 character drops the whole message to 70 units")
    void ucs2Budget() {
      String body = "π".repeat(70);
      assertThat(SmsSegment.encodingOf(body)).isEqualTo(SmsSegment.Encoding.UCS_2);
      assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
      assertThat(SmsSegment.fitsOneSegment(body + "π")).isFalse();
    }
  }

  @Nested
  @DisplayName("fitToOneSegment")
  class FitToOneSegment {

    @Test
    @DisplayName("folds a Latin-script body so it stays GSM-7 and fits")
    void foldsLatinBody() {
      String body = "Buurman: Pagamento de 1.250,00 EUR para Keizersgracht está em atraso.";

      String result = SmsSegment.fitToOneSegment(body, "Keizersgracht");

      assertThat(result).isEqualTo("Buurman: Pagamento de 1.250,00 EUR para Keizersgracht esta em atraso.");
      assertThat(SmsSegment.encodingOf(result)).isEqualTo(SmsSegment.Encoding.GSM_7);
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
    }

    @Test
    @DisplayName("truncates the value, not the sentence, when the body overflows")
    void truncatesTheValueNotTheSentence() {
      String property = "P".repeat(120);
      String body = "Buurman: Payment for " + property + " is overdue.";

      String result = SmsSegment.fitToOneSegment(body, property);

      assertThat(result).startsWith("Buurman: Payment for P");
      assertThat(result).endsWith(" is overdue.");
      assertThat(result).contains("...");
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
    }

    @Test
    @DisplayName("a Greek value in an English body forces UCS-2 and is budgeted at 70")
    void nonLatinValueFlipsTheEncoding() {
      String property = "Παλαιό Φάληρο Λεωφόρος Ποσειδώνος 42";
      String body = "Buurman: Payment of EUR 1.250,00 for " + property + " is overdue (due 15/10/2026).";

      String result = SmsSegment.fitToOneSegment(body, property);

      assertThat(SmsSegment.encodingOf(result)).isEqualTo(SmsSegment.Encoding.UCS_2);
      assertThat(SmsSegment.unitsOf(result)).isLessThanOrEqualTo(70);
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
    }

    @Test
    @DisplayName("never splits a surrogate pair")
    void neverSplitsASurrogatePair() {
      String property = "🏠".repeat(60);
      String body = "Buurman: Payment for " + property + " is overdue.";

      String result = SmsSegment.fitToOneSegment(body, property);

      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
      for (int i = 0; i < result.length(); i++) {
        char c = result.charAt(i);
        if (Character.isHighSurrogate(c)) {
          assertThat(i + 1).isLessThan(result.length());
          assertThat(Character.isLowSurrogate(result.charAt(i + 1))).isTrue();
        }
        if (Character.isLowSurrogate(c)) {
          assertThat(i).isGreaterThan(0);
          assertThat(Character.isHighSurrogate(result.charAt(i - 1))).isTrue();
        }
      }
    }

    @Test
    @DisplayName("uses an ASCII marker so truncation cannot itself flip the encoding")
    void truncationMarkerIsAscii() {
      String property = "P".repeat(200);

      String result = SmsSegment.fitToOneSegment("Buurman: " + property + " overdue.", property);

      assertThat(result).doesNotContain("…");
      assertThat(SmsSegment.encodingOf(result)).isEqualTo(SmsSegment.Encoding.GSM_7);
    }

    @Test
    @DisplayName("a body that already fits is returned folded and otherwise untouched")
    void shortBodyUntouched() {
      assertThat(SmsSegment.fitToOneSegment("Buurman: all good.", "")).isEqualTo("Buurman: all good.");
    }
  }
}
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-common -Dtest=SmsSegmentTest`
Expected: FAIL — `cannot find symbol: class SmsSegment`.

- [ ] **Step 3: Implement SmsSegment**

Create `backend/buurman-common/src/main/java/com/buurman/util/SmsSegment.java`:

```java
package com.buurman.util;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * GSM-7 folding and single-segment budgeting for SMS bodies.
 *
 * <p>An SMS costs one segment while it fits 160 septets in the GSM-7 alphabet, or 70 UTF-16 code
 * units once any character falls outside it and the message drops to UCS-2. Latin-script text is
 * therefore folded to GSM-7 first ({@code á} to {@code a}), which keeps the full 160 for twelve of
 * the thirteen languages. Greek has no acceptable Latin fold, so it keeps its own script and is
 * budgeted at 70.
 *
 * <p>The budget is always computed on the final interpolated string, never on the template: a
 * single Greek or Polish property name is enough to halve the budget of an otherwise English
 * message.
 */
public final class SmsSegment {

  /** Which alphabet a body encodes to, and therefore which budget applies. */
  public enum Encoding {
    GSM_7,
    UCS_2
  }

  private static final int GSM_7_SINGLE_SEGMENT = 160;
  private static final int UCS_2_SINGLE_SEGMENT = 70;

  /** ASCII on purpose: U+2026 is outside GSM-7 and would flip the message to UCS-2. */
  private static final String TRUNCATION_MARKER = "...";

  private static final String GSM_7_BASIC =
      "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
          + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§"
          + "¿abcdefghijklmnopqrstuvwxyzäöñüà";

  /** Present in GSM-7 only via the extension table, costing two septets each. */
  private static final String GSM_7_EXTENDED = "^{}\\[~]|€";

  /** Letters carrying no decomposable diacritic, so NFD normalisation cannot fold them. */
  private static final Map<String, String> EXPLICIT_FOLD =
      Map.ofEntries(
          Map.entry("ł", "l"),
          Map.entry("Ł", "L"),
          Map.entry("đ", "d"),
          Map.entry("Đ", "D"),
          Map.entry("ð", "d"),
          Map.entry("Ð", "D"),
          Map.entry("þ", "th"),
          Map.entry("Þ", "Th"),
          Map.entry("œ", "oe"),
          Map.entry("Œ", "OE"),
          Map.entry("ı", "i"),
          Map.entry("ŋ", "n"));

  private SmsSegment() {}

  /** Replaces characters outside GSM-7 with a GSM-7 equivalent where one exists. */
  public static String fold(String text) {
    StringBuilder folded = new StringBuilder(text.length());
    text.codePoints().forEach(codePoint -> folded.append(foldCodePoint(codePoint)));
    return folded.toString();
  }

  public static boolean isGsm7(String text) {
    return text.codePoints().allMatch(SmsSegment::isGsm7CodePoint);
  }

  public static Encoding encodingOf(String text) {
    return isGsm7(text) ? Encoding.GSM_7 : Encoding.UCS_2;
  }

  /** Septets for GSM-7 text, UTF-16 code units for UCS-2 text. */
  public static int unitsOf(String text) {
    if (encodingOf(text) == Encoding.UCS_2) {
      return text.length();
    }
    return text.codePoints().map(SmsSegment::septetsOf).sum();
  }

  public static int singleSegmentBudget(String text) {
    return encodingOf(text) == Encoding.GSM_7 ? GSM_7_SINGLE_SEGMENT : UCS_2_SINGLE_SEGMENT;
  }

  public static boolean fitsOneSegment(String text) {
    return unitsOf(text) <= singleSegmentBudget(text);
  }

  /**
   * Folds the body and, if it still overflows one segment, shortens {@code truncatableValue} — the
   * longest interpolated value — rather than the sentence around it, so the message stays
   * grammatical.
   *
   * <p>Hard-truncating the whole body is a last resort that only triggers when even a one-character
   * value overflows, which {@code SmsSegmentBudgetTest} rules out at build time for every shipped
   * body.
   */
  public static String fitToOneSegment(String body, String truncatableValue) {
    String folded = fold(body);
    if (fitsOneSegment(folded)) {
      return folded;
    }
    String foldedValue = fold(truncatableValue);
    if (!foldedValue.isBlank() && folded.contains(foldedValue)) {
      String shortened = shrinkValue(folded, foldedValue);
      if (fitsOneSegment(shortened)) {
        return shortened;
      }
      folded = shortened;
    }
    return hardTruncate(folded);
  }

  private static String foldCodePoint(int codePoint) {
    String original = new String(Character.toChars(codePoint));
    if (isGsm7(original)) {
      return original;
    }
    String explicit = EXPLICIT_FOLD.get(original);
    if (explicit != null) {
      return explicit;
    }
    String stripped = Normalizer.normalize(original, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    if (!stripped.isEmpty() && isGsm7(stripped)) {
      return stripped;
    }
    return original;
  }

  private static boolean isGsm7CodePoint(int codePoint) {
    if (codePoint > 0xFFFF) {
      return false;
    }
    char character = (char) codePoint;
    return GSM_7_BASIC.indexOf(character) >= 0 || GSM_7_EXTENDED.indexOf(character) >= 0;
  }

  private static int septetsOf(int codePoint) {
    if (codePoint <= 0xFFFF && GSM_7_EXTENDED.indexOf((char) codePoint) >= 0) {
      return 2;
    }
    return 1;
  }

  private static String shrinkValue(String body, String value) {
    int overflow = unitsOf(body) - singleSegmentBudget(body);
    int keep = value.length() - overflow - TRUNCATION_MARKER.length();
    if (keep < 1) {
      keep = 1;
    }
    return body.replace(value, cutAtGrapheme(value, keep) + TRUNCATION_MARKER);
  }

  private static String hardTruncate(String body) {
    int budget = singleSegmentBudget(body) - TRUNCATION_MARKER.length();
    if (budget < 1) {
      budget = 1;
    }
    return cutAtGrapheme(body, budget) + TRUNCATION_MARKER;
  }

  /** Cuts to at most {@code maxChars} UTF-16 units without splitting a grapheme cluster. */
  private static String cutAtGrapheme(String text, int maxChars) {
    if (text.length() <= maxChars) {
      return text;
    }
    BreakIterator boundaries = BreakIterator.getCharacterInstance(Locale.ROOT);
    boundaries.setText(text);
    int end = boundaries.preceding(maxChars + 1);
    if (end == BreakIterator.DONE || end == 0) {
      end = Math.min(maxChars, text.length());
    }
    return text.substring(0, end);
  }
}
```

- [ ] **Step 4: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-common -Dtest=SmsSegmentTest`
Expected: PASS, 13 tests.

If `nonLatinValueFlipsTheEncoding` fails because the Greek body still overflows after one shrink pass, that is the last-resort branch doing its job — check the assertion on `unitsOf(result)` rather than loosening the budget.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/util/SmsSegment.java \
        backend/buurman-common/src/test/java/com/buurman/util/SmsSegmentTest.java
git commit -m "feat(sms): add GSM-7 folding and one-segment budgeting"
```

---

### Task 3: SmsBodyRenderer and the English bundle

**Files:**
- Create: `backend/buurman-app/src/main/resources/messages/sms-bodies.properties`
- Create: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/SmsBodyRenderer.java`
- Create: `backend/buurman-notifications/src/test/java/com/buurman/service/notification/channel/SmsBodyRendererTest.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/config/EmailTemplateConfig.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/EmailSubjectResolver.java:15-19`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/LocalSmsSender.java:26-101`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/TwilioSmsSender.java`

**Interfaces:**
- Consumes: `SmsSegment.fitToOneSegment` (Task 2).
- Produces: Spring bean `SmsBodyRenderer` with `String render(String templateName, Map<String, Object> variables, Locale locale)`. Bean `notificationMessageSource` replaces `emailMessageSource`. Message keys `sms.body.<template-name>` and `sms.body.default`.

- [ ] **Step 1: Create the English SMS bundle**

Create `backend/buurman-app/src/main/resources/messages/sms-bodies.properties` with exactly the copy the senders hardcode today, so this step changes no delivered text:

```properties
sms.body.welcome=Buurman: Welcome, {userName}! Your account is ready at {baseUrl}
sms.body.verification-code=Buurman: Your code is {verificationCode}. Expires in 15 min.
sms.body.phone-verification-code=Buurman: Your phone verification code is {verificationCode}. Expires in {expiresMinutes} min.
sms.body.team-invitation=Buurman: {inviterName} invited you to {teamName}. Check your email.
sms.body.invitation-accepted=Buurman: {memberName} joined your team {teamName}.
sms.body.password-changed=Buurman: Your password was changed. Contact support if unexpected.
sms.body.payment-reminder=Buurman: Payment of {amount} for {propertyName} is overdue (due {dueDate}).
sms.body.contract-expiry=Buurman: Contract for {propertyName} expires in {daysUntilExpiry} days ({expiryDate}).
sms.body.property-created=Buurman: Property {propertyName} has been created.
sms.body.contract-created=Buurman: New contract created for {propertyName} with {contactName}.
sms.body.contract-status-changed=Buurman: Contract for {propertyName} changed from {oldStatus} to {newStatus}.
sms.body.contract-reopened=Buurman: Contract for {propertyName} ({contactName}) has been reopened for editing.
sms.body.payment-paid=Buurman: Payment of {amount} for {propertyName} has been marked as paid.
sms.body.payment-receival=Buurman: Receival of {receivalAmount} registered for {propertyName} payment.
sms.body.expense-created=Buurman: Expense of {amount} ({category}) created for {propertyName}.
sms.body.default=Buurman: You have a new notification.
```

- [ ] **Step 2: Write the failing test**

Create `backend/buurman-notifications/src/test/java/com/buurman/service/notification/channel/SmsBodyRendererTest.java`:

```java
package com.buurman.service.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.util.SmsSegment;

@DisplayName("SmsBodyRenderer")
class SmsBodyRendererTest {

  private SmsBodyRenderer renderer;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/sms-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);
    renderer = new SmsBodyRenderer(messages);
  }

  private Map<String, Object> paymentReminderVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("amount", "EUR 1.250,00");
    variables.put("propertyName", "Keizersgracht 123-B");
    variables.put("dueDate", "15/10/2026");
    return variables;
  }

  @Test
  @DisplayName("interpolates the named variables")
  void interpolatesVariables() {
    String body = renderer.render("payment-reminder", paymentReminderVariables(), Locale.ENGLISH);

    assertThat(body)
        .isEqualTo("Buurman: Payment of EUR 1.250,00 for Keizersgracht 123-B is overdue (due 15/10/2026).");
  }

  @Test
  @DisplayName("an unknown template falls back to the generic body")
  void unknownTemplateFallsBack() {
    String body = renderer.render("no-such-template", Map.of(), Locale.ENGLISH);

    assertThat(body).isEqualTo("Buurman: You have a new notification.");
  }

  @Test
  @DisplayName("never delivers a raw placeholder when a variable is missing")
  void missingVariableFallsBackRatherThanLeakingAPlaceholder() {
    Map<String, Object> incomplete = new HashMap<>();
    incomplete.put("amount", "EUR 1.250,00");

    String body = renderer.render("payment-reminder", incomplete, Locale.ENGLISH);

    assertThat(body).doesNotContain("{");
    assertThat(body).isEqualTo("Buurman: You have a new notification.");
  }

  @Test
  @DisplayName("null variables do not blow up")
  void nullVariablesAreTolerated() {
    String body = renderer.render("password-changed", null, Locale.ENGLISH);

    assertThat(body).isEqualTo("Buurman: Your password was changed. Contact support if unexpected.");
  }

  @Test
  @DisplayName("always returns a body that fits one segment")
  void alwaysFitsOneSegment() {
    Map<String, Object> variables = paymentReminderVariables();
    variables.put("propertyName", "P".repeat(300));

    String body = renderer.render("payment-reminder", variables, Locale.ENGLISH);

    assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
  }
}
```

- [ ] **Step 3: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -Dtest=SmsBodyRendererTest`
Expected: FAIL — `cannot find symbol: class SmsBodyRenderer`.

- [ ] **Step 4: Implement SmsBodyRenderer**

Create `backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/SmsBodyRenderer.java`:

```java
package com.buurman.service.notification.channel;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.buurman.util.SmsSegment;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders an SMS body from the localized {@code sms-bodies} bundle and guarantees it fits one
 * segment. Both senders delegate here, so the copy lives in one place and is actually localized —
 * they previously carried byte-identical English {@code switch} blocks and discarded the locale.
 */
@Component
@Slf4j
public class SmsBodyRenderer {

  private static final String KEY_PREFIX = "sms.body.";
  private static final String DEFAULT_KEY = KEY_PREFIX + "default";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-zA-Z0-9_]+}");

  private final MessageSource notificationMessageSource;

  public SmsBodyRenderer(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource) {
    this.notificationMessageSource = notificationMessageSource;
  }

  public String render(
      String templateName, @Nullable Map<String, Object> variables, Locale locale) {
    String body = interpolate(resolveTemplate(templateName, locale), variables);
    if (PLACEHOLDER.matcher(body).find()) {
      // A garbled message with a raw {placeholder} in it is worse than a correct generic one.
      log.warn("SMS template {} left a placeholder unfilled; using the generic body", templateName);
      body = genericBody(locale);
    }
    return SmsSegment.fitToOneSegment(body, longestValue(variables));
  }

  private String resolveTemplate(String templateName, Locale locale) {
    String key = KEY_PREFIX + templateName;
    // The notification MessageSource sets useCodeAsDefaultMessage(true), so an unknown key comes
    // back as the key itself rather than null.
    String resolved = notificationMessageSource.getMessage(key, null, key, locale);
    return key.equals(resolved) ? genericBody(locale) : resolved;
  }

  private String genericBody(Locale locale) {
    return notificationMessageSource.getMessage(DEFAULT_KEY, null, "", locale);
  }

  private String interpolate(String template, @Nullable Map<String, Object> variables) {
    if (variables == null) {
      return template;
    }
    String result = template;
    for (Map.Entry<String, Object> variable : variables.entrySet()) {
      result = result.replace("{" + variable.getKey() + "}", String.valueOf(variable.getValue()));
    }
    return result;
  }

  private String longestValue(@Nullable Map<String, Object> variables) {
    if (variables == null) {
      return "";
    }
    return variables.values().stream()
        .map(String::valueOf)
        .filter(value -> !value.isBlank())
        .max(Comparator.comparingInt(String::length))
        .orElse("");
  }
}
```

- [ ] **Step 5: Rename the message source bean and add the SMS basename**

In `EmailTemplateConfig.java`, rename the bean and register the third basename. Change the `@Bean("emailMessageSource")` method to:

```java
  @Bean("notificationMessageSource")
  public MessageSource notificationMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(
        "classpath:messages/email-subjects",
        "classpath:messages/email-bodies",
        "classpath:messages/sms-bodies");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }
```

and update the engine method's parameter and body to match:

```java
  @Bean("emailTemplateEngine")
  public TemplateEngine emailTemplateEngine(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
```

replacing every remaining `emailMessageSource` reference inside that method with `notificationMessageSource` — including both uses in the anonymous `SpringTemplateEngine` subclass.

- [ ] **Step 6: Point EmailSubjectResolver at the renamed bean**

In `EmailSubjectResolver.java`, change the field, constructor parameter and qualifier from `emailMessageSource` to `notificationMessageSource`. The field is used four times in the class body; rename all of them.

- [ ] **Step 7: Delegate from both senders**

In `LocalSmsSender.java`, add the renderer to the constructor and replace `render`, deleting `renderSmsTemplate` and `getSmsTemplate` entirely (lines 60-101):

```java
  private final MetricsService metricsService;
  private final SmsBodyRenderer smsBodyRenderer;

  public LocalSmsSender(MetricsService metricsService, SmsBodyRenderer smsBodyRenderer) {
    this.metricsService = metricsService;
    this.smsBodyRenderer = smsBodyRenderer;
  }
```

```java
  @Override
  public RenderedContent render(String templateName, Map<String, Object> variables, Locale locale) {
    return new RenderedContent(
        Optional.empty(), smsBodyRenderer.render(templateName, variables, locale), SMS);
  }
```

Apply the identical change to `TwilioSmsSender.java`: add `SmsBodyRenderer smsBodyRenderer` as a third constructor parameter after `metricsService`, assign it, replace `render` with the body above, and delete its `renderSmsTemplate` and `getSmsTemplate` methods.

- [ ] **Step 8: Run the notification module tests**

Run: `cd backend && mvn test -pl buurman-notifications`
Expected: PASS, including the 5 new `SmsBodyRendererTest` tests.

- [ ] **Step 9: Verify the whole backend still compiles and passes**

Run: `cd backend && mvn test`
Expected: PASS. The bean rename touches `EmailTemplateConfig` and `EmailSubjectResolver`; a missed reference surfaces here as a `NoSuchBeanDefinitionException` or a compile error.

- [ ] **Step 10: Commit**

```bash
git add backend/buurman-app/src/main/resources/messages/sms-bodies.properties \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/SmsBodyRenderer.java \
        backend/buurman-notifications/src/test/java/com/buurman/service/notification/channel/SmsBodyRendererTest.java \
        backend/buurman-notifications/src/main/java/com/buurman/config/EmailTemplateConfig.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/EmailSubjectResolver.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/LocalSmsSender.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/channel/TwilioSmsSender.java
git commit -m "refactor(sms): render bodies from a message bundle instead of duplicated switches"
```

---

### Task 4: SMS translations and the segment budget guard

**Files:**
- Create: `backend/buurman-app/src/main/resources/messages/sms-bodies_{nl,de,fr,pt,es,sv,it,fi,el,pl,da,nb}.properties` (12 files)
- Create: `backend/buurman-app/src/test/java/com/buurman/SmsSegmentBudgetTest.java`

**Interfaces:**
- Consumes: `SmsSegment` (Task 2), `SmsBodyRenderer` and the `sms.body.*` keys (Task 3), `DocumentLanguages.ORDERED` (Task 1).
- Produces: the 12 translated bundles.

- [ ] **Step 1: Write the failing budget test**

Create `backend/buurman-app/src/test/java/com/buurman/SmsSegmentBudgetTest.java`:

```java
package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.service.notification.channel.SmsBodyRenderer;
import com.buurman.util.DocumentLanguages;
import com.buurman.util.SmsSegment;

/**
 * Every SMS must cost exactly one segment in every language. The fixtures use deliberately long
 * values, because the budget is spent by the interpolated string, not the template.
 */
@DisplayName("every SMS body fits one segment in every language")
class SmsSegmentBudgetTest {

  private static final List<String> TEMPLATE_NAMES =
      List.of(
          "welcome",
          "verification-code",
          "phone-verification-code",
          "team-invitation",
          "invitation-accepted",
          "password-changed",
          "payment-reminder",
          "contract-expiry",
          "property-created",
          "contract-created",
          "contract-status-changed",
          "contract-reopened",
          "payment-paid",
          "payment-receival",
          "expense-created",
          "default");

  private static SmsBodyRenderer renderer() {
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/sms-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);
    return new SmsBodyRenderer(messages);
  }

  /** Worst-case but realistic values: a long Amsterdam address, a long personal name. */
  private static Map<String, Object> worstCaseVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", "Alexandra Wilhelmina");
    variables.put("baseUrl", "https://app.buurman.io");
    variables.put("verificationCode", "483920");
    variables.put("expiresMinutes", "15");
    variables.put("inviterName", "Alexandra Wilhelmina");
    variables.put("memberName", "Alexandra Wilhelmina");
    variables.put("teamName", "Amsterdam Grachtengordel Vastgoed");
    variables.put("amount", "EUR 1.250,00");
    variables.put("receivalAmount", "EUR 1.250,00");
    variables.put("propertyName", "Keizersgracht 123-B, Amsterdam");
    variables.put("contactName", "Alexandra Wilhelmina");
    variables.put("dueDate", "15/10/2026");
    variables.put("expiryDate", "15/10/2026");
    variables.put("daysUntilExpiry", "30");
    variables.put("oldStatus", "Active");
    variables.put("newStatus", "Terminated");
    variables.put("category", "Maintenance");
    return variables;
  }

  static Stream<Arguments> bodies() {
    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String templateName : TEMPLATE_NAMES) {
        cases.add(Arguments.of(language, templateName));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void fitsOneSegment(String language, String templateName) {
    String body =
        renderer().render(templateName, worstCaseVariables(), Locale.forLanguageTag(language));

    assertThat(body).as("body must not be blank").isNotBlank();
    assertThat(body).as("body must not leak a raw placeholder").doesNotContain("{");
    assertThat(SmsSegment.fitsOneSegment(body))
        .as(
            "[%s] %s is %d %s units, budget %d: %s",
            language,
            templateName,
            SmsSegment.unitsOf(body),
            SmsSegment.encodingOf(body),
            SmsSegment.singleSegmentBudget(body),
            body)
        .isTrue();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void isNotSilentlyFallingBackToTheGenericBody(String language, String templateName) {
    String generic = renderer().render("default", Map.of(), Locale.forLanguageTag(language));
    String body =
        renderer().render(templateName, worstCaseVariables(), Locale.forLanguageTag(language));

    if (!"default".equals(templateName)) {
      assertThat(body)
          .as("[%s] %s fell back to the generic body — a variable is unfilled", language, templateName)
          .isNotEqualTo(generic);
    }
  }
}
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-app -Dtest=SmsSegmentBudgetTest`
Expected: FAIL for the 12 non-English languages — with `useCodeAsDefaultMessage(true)` and no translated bundles, every non-English lookup returns the English text (the bundle falls back to the base file), so the *budget* assertions may pass while the copy is wrong. The failure that matters comes once the bundles exist. If every case passes at this point, that only confirms English fits; proceed to Step 3 and rely on the re-run in Step 5.

- [ ] **Step 3: Author the twelve translated bundles**

Create one file per language, each with all 16 `sms.body.*` keys. Rules, in priority order:

1. **Greek (`el`) has a 70-unit budget.** Its script cannot fold to GSM-7, so its copy must be terse — drop `{propertyName}` and `{dueDate}` where needed and keep the amount. Everything else keeps the full information.
2. **The other eleven get 160 septets after folding**, so natural spelling with diacritics is fine — `SmsSegment.fold` strips what GSM-7 cannot carry at render time. Do not pre-strip diacritics in the bundle; the source copy stays correctly spelled.
3. Keep the `Buurman: ` prefix and every `{variable}` token exactly as spelled in the English file. A dropped token changes meaning; the parity guard in Task 6 will reject a changed token set.
4. Use the locale's own number and date conventions in the surrounding prose only — the values themselves arrive pre-formatted.

Worked example, `sms-bodies_pt.properties`:

```properties
sms.body.welcome=Buurman: Bem-vindo, {userName}! A sua conta está pronta em {baseUrl}
sms.body.verification-code=Buurman: O seu código é {verificationCode}. Expira em 15 min.
sms.body.phone-verification-code=Buurman: O seu código de verificação é {verificationCode}. Expira em {expiresMinutes} min.
sms.body.team-invitation=Buurman: {inviterName} convidou-o para {teamName}. Verifique o seu email.
sms.body.invitation-accepted=Buurman: {memberName} juntou-se à sua equipa {teamName}.
sms.body.password-changed=Buurman: A sua palavra-passe foi alterada. Contacte o suporte se não foi você.
sms.body.payment-reminder=Buurman: Pagamento de {amount} para {propertyName} está em atraso (venc. {dueDate}).
sms.body.contract-expiry=Buurman: O contrato de {propertyName} expira em {daysUntilExpiry} dias ({expiryDate}).
sms.body.property-created=Buurman: O imóvel {propertyName} foi criado.
sms.body.contract-created=Buurman: Novo contrato criado para {propertyName} com {contactName}.
sms.body.contract-status-changed=Buurman: O contrato de {propertyName} passou de {oldStatus} para {newStatus}.
sms.body.contract-reopened=Buurman: O contrato de {propertyName} ({contactName}) foi reaberto para edição.
sms.body.payment-paid=Buurman: O pagamento de {amount} para {propertyName} foi marcado como pago.
sms.body.payment-receival=Buurman: Recebimento de {receivalAmount} registado para o pagamento de {propertyName}.
sms.body.expense-created=Buurman: Despesa de {amount} ({category}) criada para {propertyName}.
sms.body.default=Buurman: Tem uma nova notificação.
```

Worked example, `sms-bodies_el.properties` — note the deliberately shorter copy:

```properties
sms.body.welcome=Buurman: Καλώς ήρθατε, {userName}! Ο λογαριασμός σας είναι έτοιμος.
sms.body.verification-code=Buurman: Ο κωδικός σας είναι {verificationCode}. Λήγει σε 15 λεπτά.
sms.body.phone-verification-code=Buurman: Κωδικός επαλήθευσης: {verificationCode}. Λήγει σε {expiresMinutes} λεπτά.
sms.body.team-invitation=Buurman: {inviterName} σας προσκάλεσε. Δείτε το email σας.
sms.body.invitation-accepted=Buurman: {memberName} μπήκε στην ομάδα σας.
sms.body.password-changed=Buurman: Ο κωδικός σας άλλαξε. Επικοινωνήστε μαζί μας αν δεν ήσασταν εσείς.
sms.body.payment-reminder=Buurman: Καθυστερημένη πληρωμή {amount}, λήξη {dueDate}.
sms.body.contract-expiry=Buurman: Το συμβόλαιο λήγει σε {daysUntilExpiry} ημέρες ({expiryDate}).
sms.body.property-created=Buurman: Το ακίνητο {propertyName} δημιουργήθηκε.
sms.body.contract-created=Buurman: Νέο συμβόλαιο με {contactName}.
sms.body.contract-status-changed=Buurman: Το συμβόλαιο άλλαξε σε {newStatus} από {oldStatus}.
sms.body.contract-reopened=Buurman: Το συμβόλαιο {contactName} άνοιξε ξανά.
sms.body.payment-paid=Buurman: Η πληρωμή {amount} καταχωρήθηκε ως εξοφλημένη.
sms.body.payment-receival=Buurman: Είσπραξη {receivalAmount} καταχωρήθηκε.
sms.body.expense-created=Buurman: Έξοδο {amount} ({category}) καταχωρήθηκε.
sms.body.default=Buurman: Έχετε μια νέα ειδοποίηση.
```

Author the remaining ten — `nl`, `de`, `fr`, `es`, `sv`, `it`, `fi`, `pl`, `da`, `nb` — to the same contract as the `pt` file above: all 16 keys present, the `Buurman: ` prefix kept, every `{variable}` token from the English file present, and correctly spelled prose with native diacritics. Polish diacritics (`ł ą ć ę ś ż`) fold cleanly to GSM-7, so `pl` gets the full 160 like the rest — Greek is the only language on the 70-unit budget.

Note the Greek `contract-status-changed` puts `{newStatus}` before `{oldStatus}`. That is fine: the tokens are named, not positional, and the parity guard compares the token *set*.

- [ ] **Step 4: Save every file as UTF-8**

`.properties` files in this repo are UTF-8 (`setDefaultEncoding("UTF-8")`), not Latin-1. Write the characters directly; do not use `\uXXXX` escapes.

Run: `file backend/buurman-app/src/main/resources/messages/sms-bodies_el.properties`
Expected: output mentions `UTF-8`.

- [ ] **Step 5: Run the budget test**

Run: `cd backend && mvn test -pl buurman-app -Dtest=SmsSegmentBudgetTest`
Expected: PASS, 2 × 208 parameterized cases.

A failure names the language, the template, the unit count, the encoding, the budget and the offending body. Fix it by shortening that language's copy — never by widening the budget.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-app/src/main/resources/messages/sms-bodies_*.properties \
        backend/buurman-app/src/test/java/com/buurman/SmsSegmentBudgetTest.java
git commit -m "feat(sms): translate every body into the twelve non-English languages"
```

---

### Task 5: Email render matrix

**Files:**
- Create: `backend/buurman-app/src/test/java/com/buurman/EmailRenderMatrixTest.java`

**Interfaces:**
- Consumes: `DocumentLanguages.ORDERED` (Task 1).
- Produces: nothing other code depends on.

- [ ] **Step 1: Write the test**

Create `backend/buurman-app/src/test/java/com/buurman/EmailRenderMatrixTest.java`:

```java
package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.buurman.util.DocumentLanguages;

/**
 * Every email template must render in every language. Templates are discovered from disk rather
 * than listed, so a new one is covered the day it is added.
 */
@DisplayName("every email template renders in every language")
class EmailRenderMatrixTest {

  private static final Path TEMPLATE_DIR = Paths.get("src/main/resources/templates/email");

  private static SpringTemplateEngine engine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/email/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/email-subjects", "classpath:messages/email-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messages);
    return engine;
  }

  /**
   * A superset of every variable any template references. Templates take what they need and ignore
   * the rest, so one map beats twenty-four hand-maintained fixtures.
   */
  private static Map<String, Object> variables() {
    Map<String, Object> variables = new HashMap<>();
    List<String> textual =
        List.of(
            "accountHolderName", "activatedBy", "additionalDetails", "amount", "bankName",
            "baseUrl", "bicSwift", "category", "contactName", "contractIdentifier", "ctaText",
            "ctaUrl", "description", "dueDate", "effectiveFrom", "endDate", "expiresAt",
            "expiresMinutes", "expiryDate", "followUpDate", "iban", "introText", "invitationCode",
            "inviterName", "inviteUrl", "memberEmail", "memberName", "newEndDate", "newRentAmount",
            "newRentFormatted", "newStatus", "notes", "noteSubject", "oldRentAmount", "oldStatus",
            "outstanding", "paymentDate", "paymentReference", "previousEndDate",
            "previousRentFormatted", "primaryText", "primaryUrl", "propertyAddress",
            "propertyName", "propertyType", "receivalAmount", "received", "recipientName",
            "registerUrl", "remainingBalance", "renewalMode", "rentAmount", "rentChangeFormatted",
            "role", "secondaryText", "secondaryUrl", "senderName", "startDate", "teamName",
            "triggerType", "typeName", "userName", "verificationCode", "verifyUrl");
    for (String name : textual) {
      variables.put(name, "Example " + name);
    }
    variables.put("amount", "EUR 1.250,00");
    variables.put("baseUrl", "https://app.test");
    variables.put("primaryUrl", "https://app.test/primary");
    variables.put("secondaryUrl", "https://app.test/secondary");
    variables.put("count", 3);
    variables.put("truncatedCount", 1);
    variables.put("daysOverdue", 14);
    variables.put("daysRemaining", 30);
    variables.put("daysUntilExpiry", 30);
    variables.put("extensionNumber", 2);
    variables.put("renewalTermMonths", 12);
    variables.put("hasInstructions", false);
    variables.put("hasPartialPayment", false);
    variables.put("isFinal", false);
    variables.put("isOverdue", true);
    variables.put("tone", "FRIENDLY");
    variables.put("item", Map.of("label", "Example item", "value", "Example value"));
    variables.put("items", List.of(Map.of("label", "Example item", "value", "Example value")));
    variables.put("propertyNames", List.of("Example property"));
    return variables;
  }

  /** Every template except the shared fragment, which is not renderable on its own. */
  private static List<String> templateNames() throws IOException {
    try (Stream<Path> files = Files.list(TEMPLATE_DIR)) {
      return files
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".html"))
          .filter(name -> !name.startsWith("_"))
          .map(name -> name.substring(0, name.length() - ".html".length()))
          .sorted()
          .toList();
    }
  }

  static Stream<Arguments> matrix() throws IOException {
    List<String> names = templateNames();
    // A discovery bug that found nothing would otherwise make this whole suite vacuously green.
    assertThat(names).hasSizeGreaterThanOrEqualTo(24);

    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String name : names) {
        cases.add(Arguments.of(language, name));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("matrix")
  void rendersCleanly(String language, String templateName) {
    String html = engine().process(templateName, new Context(Locale.forLanguageTag(language), variables()));

    assertThat(html).as("[%s] %s rendered empty", language, templateName).isNotBlank();
    assertThat(html)
        .as("[%s] %s left an unresolved Thymeleaf message expression", language, templateName)
        .doesNotContain("#{");
    assertThat(html)
        .as("[%s] %s rendered a raw message key — the bundle is missing it", language, templateName)
        .doesNotContain("??");
    assertThat(html)
        .as("[%s] %s left a raw variable expression", language, templateName)
        .doesNotContain("${");
  }
}
```

- [ ] **Step 2: Run it**

Run: `cd backend && mvn test -pl buurman-app -Dtest=EmailRenderMatrixTest`
Expected: PASS, 312 cases (24 templates × 13 languages).

This test is expected to be green on arrival — the email bundles were audited as complete. If it is red, the audit missed something and the failure names the template, language and symptom. Fix the bundle, not the assertion.

- [ ] **Step 3: Commit**

```bash
git add backend/buurman-app/src/test/java/com/buurman/EmailRenderMatrixTest.java
git commit -m "test(i18n): render every email template in every language"
```

---

### Task 6: Bundle parity guard and the legal clause translations

**Files:**
- Create: `backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-extension_{da,el,fi,nb,pl}.properties`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-rent-change_{da,el,fi,nb,pl}.properties`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-deposit-statement_{nl,de,fr,pt,es,sv,it,fi,el,pl,da,nb}.properties`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-payment-notice_{nl,de,fr,pt,es,sv,it,fi,el,pl,da,nb}.properties`

**Interfaces:**
- Consumes: `DocumentLanguages.ORDERED` (Task 1); the `sms-bodies` family (Tasks 3-4).
- Produces: nothing other code depends on.

- [ ] **Step 1: Confirm where the letter bundles live**

Run: `find backend -path '*resources/messages*' -name 'document-extension*' -not -path '*/target/*'`
Expected: a list of 13 files. Use the directory it reports for every path in this task — the plan assumes `buurman-letters`, and if these bundles sit in `buurman-booklets` instead, adjust the paths and nothing else.

- [ ] **Step 2: Write the failing parity test**

Create `backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java`:

```java
package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.buurman.util.DocumentLanguages;

/**
 * Every message bundle must carry every key in every language, with the same placeholders.
 *
 * <p>Families are discovered from the classpath rather than listed, so a new bundle is covered the
 * day it is added. The notification MessageSource sets useCodeAsDefaultMessage(true), which means a
 * missing key is not a blank — it renders the literal key into a delivered email.
 */
@DisplayName("every message bundle is complete in every language")
class I18nBundleParityTest {

  /** {0}-style MessageFormat arguments and {named} SMS tokens alike. */
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

  private static final String BASE_LANGUAGE = "en";

  private record Family(String name, Map<String, Resource> byLanguage) {}

  private static List<Family> discover() throws IOException {
    Resource[] resources =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:messages/*.properties");

    Map<String, Map<String, Resource>> families = new LinkedHashMap<>();
    for (Resource resource : resources) {
      String fileName = resource.getFilename();
      if (fileName == null) {
        continue;
      }
      String base = fileName.substring(0, fileName.length() - ".properties".length());
      // Fixtures used by other tests, not shipped copy.
      if (base.startsWith("test-")) {
        continue;
      }
      String family = base;
      String language = BASE_LANGUAGE;
      int underscore = base.lastIndexOf('_');
      if (underscore > 0 && base.length() - underscore == 3) {
        family = base.substring(0, underscore);
        language = base.substring(underscore + 1);
      }
      families.computeIfAbsent(family, key -> new LinkedHashMap<>()).put(language, resource);
    }

    List<Family> discovered = new ArrayList<>();
    families.forEach((name, byLanguage) -> discovered.add(new Family(name, byLanguage)));
    return discovered;
  }

  private static Properties load(Resource resource) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static Set<String> placeholders(String value) {
    Set<String> found = new TreeSet<>();
    Matcher matcher = PLACEHOLDER.matcher(value);
    while (matcher.find()) {
      found.add(matcher.group(1));
    }
    return found;
  }

  static Stream<Arguments> families() throws IOException {
    List<Family> discovered = discover();
    // A discovery bug that found nothing would otherwise make this whole suite vacuously green.
    assertThat(discovered)
        .as("classpath scan found no message bundles at all")
        .hasSizeGreaterThanOrEqualTo(13);
    return discovered.stream().map(Arguments::of);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  @DisplayName("carries every language")
  void carriesEveryLanguage(Family family) {
    assertThat(family.byLanguage().keySet())
        .as("%s is missing language files", family.name())
        .containsAll(DocumentLanguages.ORDERED);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  @DisplayName("every language carries every key, with the same placeholders")
  void everyLanguageCarriesEveryKey(Family family) throws IOException {
    Properties base = load(family.byLanguage().get(BASE_LANGUAGE));
    Set<String> baseKeys = new TreeSet<>(base.stringPropertyNames());

    for (String language : DocumentLanguages.ORDERED) {
      if (BASE_LANGUAGE.equals(language)) {
        continue;
      }
      Properties translated = load(family.byLanguage().get(language));
      Set<String> translatedKeys = new TreeSet<>(translated.stringPropertyNames());

      assertThat(translatedKeys)
          .as("%s [%s] is missing keys", family.name(), language)
          .containsAll(baseKeys);
      assertThat(baseKeys)
          .as("%s [%s] has keys the base bundle does not", family.name(), language)
          .containsAll(translatedKeys);

      for (String key : baseKeys) {
        assertThat(placeholders(translated.getProperty(key)))
            .as("%s [%s] %s changed its placeholders", family.name(), language, key)
            .isEqualTo(placeholders(base.getProperty(key)));
      }
    }
  }
}
```

- [ ] **Step 3: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-app -Dtest=I18nBundleParityTest`
Expected: FAIL. `everyLanguageCarriesEveryKey` reports missing keys for `document-extension`, `document-rent-change`, `document-deposit-statement` and `document-payment-notice` — 380 entries in total. Each failure names the family, language and the specific keys.

- [ ] **Step 4: List exactly what is missing**

Run:

```bash
cd backend && for fam in document-extension document-rent-change document-deposit-statement document-payment-notice; do
  base=$(find . -path '*resources/messages*' -name "$fam.properties" -not -path '*/target/*' | head -1)
  for f in $(find . -path '*resources/messages*' -name "${fam}_*.properties" -not -path '*/target/*' | sort); do
    missing=$(comm -23 <(grep -oE '^[a-zA-Z0-9._]+' "$base" | sort) <(grep -oE '^[a-zA-Z0-9._]+' "$f" | sort))
    if [ -n "$missing" ]; then echo "== $f"; echo "$missing"; fi
  done
done
```

Expected: 34 file sections listing the `*.legal.<COUNTRY>` keys each one lacks.

- [ ] **Step 5: Translate the missing legal clauses**

For each missing key, take the English clause from the family's base `.properties` file and append a translated entry to the corresponding language file, preserving the exact key and any `{placeholder}` tokens.

These are jurisdiction-specific legal clauses in tenant-facing letters. Translate faithfully and conservatively: keep statute names, article numbers, and monetary and date placeholders exactly as the English clause has them, and do not adapt the legal substance to the reader's country — `legal.AT` is Austrian law whatever language it is read in. A machine-translation pass is the expected starting point; Task 11 records these for native legal review.

Keep each file's existing key order and grouping, appending new keys next to their neighbours rather than at the end of the file.

- [ ] **Step 6: Run the parity test**

Run: `cd backend && mvn test -pl buurman-app -Dtest=I18nBundleParityTest`
Expected: PASS, 2 × 13 families.

- [ ] **Step 7: Run the whole backend suite**

Run: `cd backend && mvn test`
Expected: PASS. Confirms the new bundle entries did not break letter rendering.

- [ ] **Step 8: Commit**

```bash
git add backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java
git commit -m "test(i18n): fail the build on any incomplete message bundle"
git add backend/buurman-letters/src/main/resources/messages/
git commit -m "feat(letters): translate the jurisdiction legal clauses into every language"
```

---

### Task 7: Frontend locale parity guard

**Files:**
- Create: `frontend/app/src/i18n/__tests__/locales.parity.test.ts`
- Modify: `frontend/app/public/locales/{nl,de,fr,pt,es,sv,it,fi,el,pl,da,nb}/contracts.json`
- Modify: `frontend/app/public/locales/nb/properties.json`
- Modify: `frontend/app/public/locales/en/payments.json`

**Interfaces:**
- Consumes: `SUPPORTED_LANGUAGES` from `src/config/languages.ts` (Task 1).
- Produces: nothing other code depends on.

- [ ] **Step 1: Write the failing test**

Create `frontend/app/src/i18n/__tests__/locales.parity.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { SUPPORTED_LANGUAGES } from '../../config/languages';

const LOCALES_DIR = path.resolve(import.meta.dirname, '../../../public/locales');
const BASE_LANGUAGE = 'en';

type Json = Record<string, unknown>;

const readNamespace = (language: string, namespace: string): Json =>
  JSON.parse(fs.readFileSync(path.join(LOCALES_DIR, language, namespace), 'utf8')) as Json;

const flatten = (value: Json, prefix = ''): string[] =>
  Object.entries(value).flatMap(([key, child]) =>
    child !== null && typeof child === 'object' && !Array.isArray(child)
      ? flatten(child as Json, `${prefix}${key}.`)
      : [`${prefix}${key}`]
  );

// i18next appends a CLDR plural category to keys that interpolate a count. Which categories a
// language needs differs — Polish needs one/few/many/other where English needs one/other — so the
// suffix is stripped before comparing keys and checked separately below.
const PLURAL_SUFFIX = /_(zero|one|two|few|many|other)$/;
const stripPlural = (key: string) => key.replace(PLURAL_SUFFIX, '');

const namespaces = fs
  .readdirSync(path.join(LOCALES_DIR, BASE_LANGUAGE))
  .filter((name) => name.endsWith('.json'))
  .sort();

const pluralCategories = (language: string): string[] => {
  const rules = new Intl.PluralRules(language);
  return [...(rules.resolvedOptions().pluralCategories ?? [])].sort();
};

describe('locale bundles', () => {
  it('discovers the namespaces it is meant to check', () => {
    expect(namespaces.length).toBeGreaterThanOrEqual(10);
  });

  it('ships a directory for every supported language and nothing else', () => {
    const directories = fs
      .readdirSync(LOCALES_DIR, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => entry.name)
      .sort();

    expect(directories).toEqual([...SUPPORTED_LANGUAGES].sort());
  });

  describe.each(SUPPORTED_LANGUAGES.filter((language) => language !== BASE_LANGUAGE))(
    '%s',
    (language) => {
      it.each(namespaces)('%s carries every key', (namespace) => {
        const base = new Set(flatten(readNamespace(BASE_LANGUAGE, namespace)).map(stripPlural));
        const translated = new Set(
          flatten(readNamespace(language, namespace)).map(stripPlural)
        );

        expect([...base].filter((key) => !translated.has(key))).toEqual([]);
        expect([...translated].filter((key) => !base.has(key))).toEqual([]);
      });
    }
  );

  it.each(SUPPORTED_LANGUAGES)(
    '%s supplies exactly the plural categories its language needs',
    (language) => {
      const expected = pluralCategories(language);

      for (const namespace of namespaces) {
        const keys = flatten(readNamespace(language, namespace));
        const pluralBases = new Set(
          keys.filter((key) => PLURAL_SUFFIX.test(key)).map(stripPlural)
        );

        for (const base of pluralBases) {
          const supplied = keys
            .filter((key) => stripPlural(key) === base && PLURAL_SUFFIX.test(key))
            .map((key) => key.slice(base.length + 1))
            .sort();

          expect(supplied, `${language}/${namespace} ${base}`).toEqual(expected);
        }
      }
    }
  );
});
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd frontend && yarn test --filter app -- locales.parity`
Expected: FAIL — `contracts.json` missing `countryMetadata.fields.depositSum` in all 12 languages, `nb/properties.json` missing three `map.*` keys, and the plural check failing where a base key has partial categories.

If the `--filter` form is not supported by this workspace setup, run `cd frontend/app && yarn vitest run src/i18n/__tests__/locales.parity.test.ts` instead.

- [ ] **Step 3: Add the missing contracts key**

Read the English value:

Run: `cd frontend/app/public/locales && python3 -c "import json;print(json.load(open('en/contracts.json'))['countryMetadata']['fields']['depositSum'])"`

Then add a translated `countryMetadata.fields.depositSum` to each of the 12 language files, in the same position within the `countryMetadata.fields` object as the English file has it.

- [ ] **Step 4: Add the missing Norwegian properties keys**

Read the three English values:

Run: `cd frontend/app/public/locales && python3 -c "import json;m=json.load(open('en/properties.json'))['map'];print({k:m[k] for k in ('mapView','noStreetView','streetView')})"`

Then add translated `map.mapView`, `map.noStreetView` and `map.streetView` to `nb/properties.json`.

- [ ] **Step 5: Fix the English plural bug**

`en/payments.json` has `selection.selected` interpolating `{{count}}` with no plural forms, while `pt`, `es`, `fr`, `it`, `sv` and `el` correctly supply `selected_other`. English is the broken one. In `en/payments.json`, replace:

```json
      "selected": "{{count}} selected"
```

with:

```json
      "selected_one": "{{count}} selected",
      "selected_other": "{{count}} selected"
```

Then give every other language the full set of categories its language needs for that key, as the test's `Intl.PluralRules` check reports — Polish needs `one`, `few`, `many` and `other`; Greek and the Romance and Germanic languages need `one` and `other`.

- [ ] **Step 6: Run the test**

Run: `cd frontend && yarn test`
Expected: PASS, all app and backoffice tests including the new parity suite.

- [ ] **Step 7: Lint and commit**

```bash
cd frontend && yarn lint --fix && cd ..
git add frontend/app/src/i18n/__tests__/locales.parity.test.ts frontend/app/public/locales/
git commit -m "test(i18n): fail the build on locale namespace drift, and close the existing gaps"
```

---

### Task 8: `preferred_language` on contacts

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V073__contact_preferred_language.sql`
- Modify: `backend/buurman-common/src/main/java/com/buurman/domain/Contact.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/request/CreateContactRequest.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateContactRequest.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/response/ContactResponse.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/mapper/ContactRecordMapper.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/repository/ContactRepository.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContactService.java`
- Modify: `openapi/src/app.yaml`
- Create: `backend/buurman-core/src/test/java/com/buurman/repository/ContactRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `LanguageCode` schema and `DocumentLanguages` (Task 1).
- Produces: `Contact.getPreferredLanguage()` returning `Optional<String>`; `ContactResponse.preferredLanguage()`; `CreateContactRequest`/`UpdateContactRequest` gain a trailing `Optional<String> preferredLanguage` component.

- [ ] **Step 1: Write the migration**

Create `backend/buurman-jooq/src/main/resources/db/migration/V073__contact_preferred_language.sql`:

```sql
-- The language a contact should be written to in. Resolution order at send time is
-- contact preference, then the recipient user's preference, then the contract's document
-- language, then the team default, then English.
ALTER TABLE contacts
    ADD COLUMN preferred_language VARCHAR(2);

ALTER TABLE contacts
    ADD CONSTRAINT chk_contact_preferred_language
        CHECK (preferred_language IS NULL OR preferred_language IN
               ('en', 'nl', 'de', 'fr', 'pt', 'es', 'sv', 'it', 'fi', 'el', 'pl', 'da', 'nb'));
```

- [ ] **Step 2: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`
Expected: `CONTACTS.PREFERRED_LANGUAGE` exists in the generated sources. Docker must be running.

- [ ] **Step 3: Write the failing integration test**

Create `backend/buurman-core/src/test/java/com/buurman/repository/ContactRepositoryIntegrationTest.java`:

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contact;
import com.buurman.mapper.ContactRecordMapperImpl;

@DisplayName("ContactRepository Integration")
class ContactRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContactRepository repo;

  @BeforeEach
  void setUp() {
    repo = new ContactRepository(dsl, new ContactRecordMapperImpl(), CLOCK);
  }

  @Test
  @DisplayName("preferred language round-trips")
  void preferredLanguageRoundTrips() {
    Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
    contact.setPreferredLanguage(Optional.of("pt"));

    Contact saved = repo.save(contact);

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
        .get()
        .extracting(Contact::getPreferredLanguage)
        .isEqualTo(Optional.of("pt"));
  }

  @Test
  @DisplayName("preferred language defaults to empty")
  void preferredLanguageDefaultsToEmpty() {
    Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
        .get()
        .extracting(Contact::getPreferredLanguage)
        .isEqualTo(Optional.empty());
  }

  @Test
  @DisplayName("an unsupported language code is rejected by the database")
  void unsupportedLanguageIsRejected() {
    Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
    contact.setPreferredLanguage(Optional.of("xx"));

    assertThatThrownBy(() -> repo.save(contact))
        .hasMessageContaining("chk_contact_preferred_language");
  }

  @Test
  @DisplayName("a contact is invisible to another team")
  void contactIsScopedToItsTeam() {
    Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_B_ID)).isEmpty();
  }
}
```

- [ ] **Step 4: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-core -Dtest=ContactRepositoryIntegrationTest`
Expected: FAIL — `cannot find symbol: method setPreferredLanguage`.

- [ ] **Step 5: Add the field to the domain POJO**

In `Contact.java`, immediately after the `paymentRemindersEnabled` field, add:

```java
  /** ISO 639-1 code this contact should be written to in. Empty means fall through to the team. */
  @Builder.Default private Optional<String> preferredLanguage = Optional.empty();
```

- [ ] **Step 6: Add it to the mapper**

In `ContactRecordMapper.java`, add another `@Mapping` alongside the existing Optional mappings:

```java
  @Mapping(
      target = "preferredLanguage",
      expression = "java(java.util.Optional.ofNullable(record.getPreferredLanguage()))")
```

- [ ] **Step 7: Add it to the repository**

In `ContactRepository.java`, add to the insert builder immediately after the `PAYMENT_REMINDERS_ENABLED` line:

```java
          .set(CONTACTS.PREFERRED_LANGUAGE, contact.getPreferredLanguage().orElse(null))
```

and add the identical line to the update builder, again next to its `PAYMENT_REMINDERS_ENABLED` line.

- [ ] **Step 8: Run the integration test**

Run: `cd backend && mvn test -pl buurman-core -Dtest=ContactRepositoryIntegrationTest`
Expected: PASS, 4 tests.

- [ ] **Step 9: Thread it through the DTOs**

Add a trailing component to each record, after `paymentRemindersEnabled`:

- `CreateContactRequest`: `Optional<String> preferredLanguage`
- `UpdateContactRequest`: `Optional<String> preferredLanguage`
- `ContactResponse`: `Optional<String> preferredLanguage`

- [ ] **Step 10: Thread it through ContactService**

`ContactService` builds `UpdateContactRequest` positionally in two `switch` arms and maps `Contact` to `ContactResponse`. Add `request.preferredLanguage()` as the final argument to both `new UpdateContactRequest(...)` calls, add `.preferredLanguage(contact.getPreferredLanguage())` to `cloneContact`, and add `.preferredLanguage(contact.getPreferredLanguage())` next to the existing `.paymentRemindersEnabled(...)` line in the response mapping around line 1004. The compiler will point at any create/update path still missing the argument.

- [ ] **Step 11: Compile and run the module**

Run: `cd backend && mvn test -pl buurman-core`
Expected: PASS.

- [ ] **Step 12: Add it to OpenAPI**

In `openapi/src/app.yaml`, add to all three contact schemas — `UpdateContactRequest` (~line 1015), `ContactResponse` (~line 1236) and `CreateContactRequest` (~line 4830) — immediately after each one's `paymentRemindersEnabled` property:

```yaml
        preferredLanguage:
          allOf:
            - $ref: '#/components/schemas/LanguageCode'
          description: Language this contact is written to in; unset falls back to the team default
```

The `allOf` wrapper is not decoration: this is OpenAPI 3.0.1, where any sibling of a `$ref` is
ignored, so a `description` written directly beside the `$ref` would silently disappear.

While in `UpdateContactRequest`, delete the stray `format: email` line under its `paymentRemindersEnabled` boolean — it is meaningless on a boolean and was a copy-paste slip.

- [ ] **Step 13: Bundle, regenerate, build**

Run: `make bundle-openapi && cd frontend && yarn generate:api && yarn build`
Expected: succeeds; the generated `ContactResponse` type carries `preferredLanguage`.

- [ ] **Step 14: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V073__contact_preferred_language.sql \
        backend/buurman-common/src/main/java/com/buurman/domain/Contact.java \
        backend/buurman-common/src/main/java/com/buurman/dto/request/CreateContactRequest.java \
        backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateContactRequest.java \
        backend/buurman-common/src/main/java/com/buurman/dto/response/ContactResponse.java \
        backend/buurman-core/src/main/java/com/buurman/mapper/ContactRecordMapper.java \
        backend/buurman-core/src/main/java/com/buurman/repository/ContactRepository.java \
        backend/buurman-core/src/main/java/com/buurman/service/ContactService.java \
        backend/buurman-core/src/test/java/com/buurman/repository/ContactRepositoryIntegrationTest.java \
        openapi/src/app.yaml openapi/app.yaml
git commit -m "feat(contacts): add a preferred language"
```

---

### Task 9: RecipientLocaleResolver

**Files:**
- Create: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/RecipientLocaleResolver.java`
- Create: `backend/buurman-notifications/src/test/java/com/buurman/service/notification/RecipientLocaleResolverTest.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/notification/SendNotificationRequest.java`
- Modify: `backend/buurman-notifications/src/main/java/com/buurman/service/notification/NotificationServiceImpl.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/PaymentReminderService.java:434`
- Modify: `backend/buurman-core/src/test/java/com/buurman/service/PaymentReminderServiceTest.java:321`

**Interfaces:**
- Consumes: `Contact.getPreferredLanguage()` (Task 8); `ContactRepository.findByIdAndTeamId` and `UserPreferencesRepository.findByUserId` and `TeamPreferencesRepository.findByTeamId` (existing).
- Produces: Spring bean `RecipientLocaleResolver` with `Locale resolve(Optional<UUID> teamId, Optional<UUID> contactId, Optional<UUID> userId, Optional<String> contextLanguageTag)`. `SendNotificationRequest.languageTag` is renamed `contextLanguageTag`.

- [ ] **Step 1: Write the failing test**

Create `backend/buurman-notifications/src/test/java/com/buurman/service/notification/RecipientLocaleResolverTest.java`:

```java
package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contact;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserPreferencesRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipientLocaleResolver")
class RecipientLocaleResolverTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private ContactRepository contactRepository;
  @Mock private UserPreferencesRepository userPreferencesRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;

  private RecipientLocaleResolver resolver;

  @BeforeEach
  void setUp() {
    resolver =
        new RecipientLocaleResolver(
            contactRepository, userPreferencesRepository, teamPreferencesRepository);
  }

  private Contact contactWithLanguage(Optional<String> language) {
    Contact contact = new Contact();
    contact.setPreferredLanguage(language);
    return contact;
  }

  private void teamDefaultIs(String language) {
    TeamPreferences preferences = new TeamPreferences();
    preferences.setDefaultLanguage(language);
    lenient().when(teamPreferencesRepository.findByTeamId(TEAM_ID)).thenReturn(Optional.of(preferences));
  }

  @Test
  @DisplayName("the contact's own language wins")
  void contactLanguageWins() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.of("pt"))));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("pt"));
  }

  @Test
  @DisplayName("a contact with no language of its own falls through")
  void nullContactLanguageFallsThrough() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.empty())));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("de"));
  }

  @Test
  @DisplayName("a contact that is not in this team falls through rather than leaking")
  void contactFromAnotherTeamFallsThrough() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID)).thenReturn(Optional.empty());

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("no team means no contact lookup at all")
  void noTeamMeansNoContactLookup() {
    Locale locale =
        resolver.resolve(
            Optional.empty(), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("fr"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("fr"));
    verifyNoInteractions(contactRepository);
  }

  @Test
  @DisplayName("the recipient user's preference beats the context language")
  void userPreferenceBeatsContext() {
    UserPreferences preferences = new UserPreferences();
    preferences.setLanguage("sv");
    when(userPreferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(preferences));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.empty(), Optional.of(USER_ID), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("sv"));
  }

  @Test
  @DisplayName("the context language beats the team default")
  void contextBeatsTeamDefault() {
    teamDefaultIs("nl");

    Locale locale =
        resolver.resolve(Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("de"));
  }

  @Test
  @DisplayName("a blank context language is ignored")
  void blankContextIsIgnored() {
    teamDefaultIs("nl");

    Locale locale =
        resolver.resolve(Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.of("  "));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("an unsupported language anywhere in the chain is ignored")
  void unsupportedLanguageIsIgnored() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.of("klingon"))));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("with nothing to go on it falls back to English")
  void fallsBackToEnglish() {
    when(teamPreferencesRepository.findByTeamId(any())).thenReturn(Optional.empty());

    Locale locale =
        resolver.resolve(Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.ENGLISH);
  }
}
```

If `TeamPreferences` or `UserPreferences` use a builder rather than setters, adjust the two helper methods to match — check the domain classes before running.

- [ ] **Step 2: Run it to make sure it fails**

Run: `cd backend && mvn test -pl buurman-notifications -Dtest=RecipientLocaleResolverTest`
Expected: FAIL — `cannot find symbol: class RecipientLocaleResolver`.

- [ ] **Step 3: Implement the resolver**

Create `backend/buurman-notifications/src/main/java/com/buurman/service/notification/RecipientLocaleResolver.java`:

```java
package com.buurman.service.notification;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.util.DocumentLanguages;

import lombok.RequiredArgsConstructor;

/**
 * Decides which language a notification is written in.
 *
 * <p>The order is: the contact's own preference, then the recipient user's, then the caller's
 * context language (a contract's document language), then the team default, then English. The
 * contact comes first because a tenant's own language beats the language the landlord happens to
 * file contracts in.
 *
 * <p>Both the send path and the resend path use this, so a resent reminder cannot come back in a
 * different language than the original.
 */
@Component
@RequiredArgsConstructor
public class RecipientLocaleResolver {

  private final ContactRepository contactRepository;
  private final UserPreferencesRepository userPreferencesRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;

  public Locale resolve(
      Optional<UUID> teamId,
      Optional<UUID> contactId,
      Optional<UUID> userId,
      Optional<String> contextLanguageTag) {
    return contactLanguage(teamId, contactId)
        .or(() -> userLanguage(userId))
        .or(() -> supported(contextLanguageTag))
        .or(() -> teamLanguage(teamId))
        .map(Locale::forLanguageTag)
        .orElse(Locale.ENGLISH);
  }

  /** Scoped by team on purpose: a contact id from another team must not resolve to its language. */
  private Optional<String> contactLanguage(Optional<UUID> teamId, Optional<UUID> contactId) {
    return teamId.flatMap(
        team ->
            contactId
                .flatMap(contact -> contactRepository.findByIdAndTeamId(contact, team))
                .flatMap(Contact::getPreferredLanguage)
                .flatMap(language -> supported(Optional.of(language))));
  }

  private Optional<String> userLanguage(Optional<UUID> userId) {
    return userId
        .flatMap(userPreferencesRepository::findByUserId)
        .map(UserPreferences::getLanguage)
        .flatMap(language -> supported(Optional.ofNullable(language)));
  }

  private Optional<String> teamLanguage(Optional<UUID> teamId) {
    return teamId
        .flatMap(teamPreferencesRepository::findByTeamId)
        .map(TeamPreferences::getDefaultLanguage)
        .flatMap(language -> supported(Optional.ofNullable(language)));
  }

  private Optional<String> supported(Optional<String> languageTag) {
    return languageTag.map(String::trim).filter(DocumentLanguages::isSupported);
  }
}
```

- [ ] **Step 4: Run it to make sure it passes**

Run: `cd backend && mvn test -pl buurman-notifications -Dtest=RecipientLocaleResolverTest`
Expected: PASS, 9 tests.

- [ ] **Step 5: Rename the request field**

In `SendNotificationRequest.java`, rename the component and its builder default, and correct the now-wrong Javadoc:

```java
    /**
     * Context language for rendering (BCP 47 tag) — typically a contract's document language.
     * Ranked below the recipient's own preference, not an override.
     */
    Optional<String> contextLanguageTag,
```

and in the builder class:

```java
    private Optional<String> contextLanguageTag = Optional.empty();
```

- [ ] **Step 6: Use the resolver in both paths**

In `NotificationServiceImpl.java`:

1. Add `RecipientLocaleResolver recipientLocaleResolver` as a constructor parameter and field, assigning it alongside the others.
2. Replace the `resolveRecipientLocale(request)` call in `send` with:

```java
      Locale recipientLocale =
          recipientLocaleResolver.resolve(
              request.teamId(),
              request.recipientContactId(),
              request.recipientUserId(),
              request.contextLanguageTag());
```

3. Delete the private `resolveRecipientLocale` method entirely.
4. In the resend path, delete the `resentUserLang` and `resentLocale` blocks and replace them with:

```java
    Locale resentLocale =
        recipientLocaleResolver.resolve(
            original.getTeamId(),
            original.getRecipientContactId(),
            original.getRecipientUserId(),
            Optional.empty());
```

5. Remove any imports left unused by those deletions.

- [ ] **Step 7: Update the one caller**

In `PaymentReminderService.java:434`, change `.languageTag(Optional.of(languageTag))` to `.contextLanguageTag(Optional.of(languageTag))`. In `PaymentReminderServiceTest.java:321`, change `sent.languageTag()` to `sent.contextLanguageTag()`.

Run: `cd backend && grep -rn "languageTag(" --include=*.java backend | grep -v contextLanguageTag`
Expected: no remaining `.languageTag(` builder calls or accessor uses outside the renamed field.

- [ ] **Step 8: Run the full backend suite**

Run: `cd backend && mvn test`
Expected: PASS. `PaymentReminderServiceTest` still asserts the request carries `nl`; the re-rank is behaviour-preserving for it because that test supplies no contact preference.

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-notifications/src/main/java/com/buurman/service/notification/RecipientLocaleResolver.java \
        backend/buurman-notifications/src/test/java/com/buurman/service/notification/RecipientLocaleResolverTest.java \
        backend/buurman-core/src/main/java/com/buurman/service/notification/SendNotificationRequest.java \
        backend/buurman-notifications/src/main/java/com/buurman/service/notification/NotificationServiceImpl.java \
        backend/buurman-core/src/main/java/com/buurman/service/PaymentReminderService.java \
        backend/buurman-core/src/test/java/com/buurman/service/PaymentReminderServiceTest.java
git commit -m "feat(notifications): resolve recipient language from the contact first"
```

---

### Task 10: Contact language selector

**Files:**
- Modify: `frontend/app/src/components/contacts/ContactForm.tsx`
- Modify: `frontend/app/src/pages/ContactDetailPage.tsx`
- Modify: `frontend/app/public/locales/*/tenants.json` (13 files)

**Interfaces:**
- Consumes: `SUPPORTED_LANGUAGES` (Task 1); the generated `ContactResponse.preferredLanguage` (Task 8).
- Produces: nothing other code depends on.

- [ ] **Step 1: Add the form state**

In `ContactForm.tsx`, add to the initial form state next to `paymentRemindersEnabled` (around line 87):

```ts
    preferredLanguage: contact?.preferredLanguage ?? '',
```

and the identical line to the `useEffect` sync block around line 114.

- [ ] **Step 2: Add the select**

In `ContactForm.tsx`, import the canonical list and `useTranslation` if not already imported:

```ts
import { SUPPORTED_LANGUAGES } from '@/config/languages';
```

Add this field next to the payment-reminders checkbox (around line 435), following the form's existing field markup:

```tsx
            <div>
              <label
                htmlFor="preferredLanguage"
                className="block text-sm font-medium text-text-primary mb-1"
              >
                {t('form.preferredLanguage')}
              </label>
              <select
                id="preferredLanguage"
                value={formData.preferredLanguage ?? ''}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    preferredLanguage: e.target.value,
                  }))
                }
                className="w-full rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm focus-ring"
              >
                <option value="">{t('form.preferredLanguageDefault')}</option>
                {SUPPORTED_LANGUAGES.map((language) => (
                  <option key={language} value={language}>
                    {new Intl.DisplayNames([language], { type: 'language' }).of(language) ??
                      language}
                  </option>
                ))}
              </select>
              <p className="mt-1 text-xs text-text-muted">{t('form.preferredLanguageHint')}</p>
            </div>
```

- [ ] **Step 3: Send it on submit**

In `ContactForm.tsx`, find where the submit handler builds its payload from `formData` and add:

```ts
      preferredLanguage: formData.preferredLanguage || undefined,
```

so an unset selection is omitted rather than sent as an empty string, which the `LanguageCode` enum would reject.

- [ ] **Step 4: Show it on the detail page**

In `ContactDetailPage.tsx`, next to where `paymentRemindersEnabled` is displayed, render the contact's language when set:

```tsx
              {contact.preferredLanguage && (
                <div>
                  <dt className="text-sm text-text-muted">{t('form.preferredLanguage')}</dt>
                  <dd className="text-sm font-medium text-text-primary">
                    {new Intl.DisplayNames([contact.preferredLanguage], { type: 'language' }).of(
                      contact.preferredLanguage
                    ) ?? contact.preferredLanguage}
                  </dd>
                </div>
              )}
```

- [ ] **Step 5: Add the three translation keys**

Add to `en/tenants.json` under `form`:

```json
      "preferredLanguage": "Language",
      "preferredLanguageDefault": "Use the team default",
      "preferredLanguageHint": "Emails and text messages to this contact are written in this language."
```

Then add translations of all three to the other 12 `tenants.json` files. The Task 7 parity test fails until every language has them.

- [ ] **Step 6: Verify**

Run: `cd frontend && yarn lint --fix && yarn test && yarn build`
Expected: all pass, including the locale parity suite.

- [ ] **Step 7: Commit**

```bash
git add frontend/app/src/components/contacts/ContactForm.tsx \
        frontend/app/src/pages/ContactDetailPage.tsx \
        frontend/app/public/locales/
git commit -m "feat(contacts): let a landlord set the language a contact is written to in"
```

---

### Task 11: Translation review checklist

**Files:**
- Create: `docs/i18n-review.md`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Write the checklist**

Create `docs/i18n-review.md`:

```markdown
# Translation review

Roughly 600 strings were added machine-translated in BUUR-109 and need a native
speaker's pass. This file tracks that pass. Tick a language only when someone who
speaks it has read every listed bundle end to end.

## What was added

| Surface | Strings | Risk |
| --- | --- | --- |
| `messages/sms-bodies_*.properties` | 208 (16 × 13) | Low. Short, factual, and the budget test proves each fits one segment. |
| `document-extension`, `document-rent-change`, `document-deposit-statement`, `document-payment-notice` — the `*.legal.<COUNTRY>` keys | 380 | **High. Jurisdiction legal clauses in tenant-facing letters.** |
| `public/locales/*/contracts.json`, `properties.json`, `payments.json`, `tenants.json` | ~50 | Low. UI labels. |

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
  than 160. Do not "restore" the detail the English version carries.
- Plural forms cover the categories the language actually needs. `yarn test` enforces
  this; if it complains about a language you are reviewing, it is right.
```

- [ ] **Step 2: Commit**

```bash
git add docs/i18n-review.md
git commit -m "docs(i18n): add the native-review checklist for the new translations"
```

---

## Final verification

- [ ] **Run the whole backend suite**

Run: `cd backend && mvn test`
Expected: PASS. Docker must be running for the integration tests.

- [ ] **Run the whole frontend suite**

Run: `cd frontend && yarn test && yarn lint && yarn build`
Expected: PASS.

- [ ] **Confirm the acceptance criteria this slice owns**

- A contact with `preferredLanguage=pt` receives the payment reminder in Portuguese: covered by `RecipientLocaleResolverTest.contactLanguageWins` plus `EmailRenderMatrixTest` and `SmsSegmentBudgetTest` for `pt`.
- A landlord whose user preference is `nl` receives "payment received" in Dutch: covered by `RecipientLocaleResolverTest.userPreferenceBeatsContext`.
- Every template renders in all 13 locales in CI with no missing keys: covered by `EmailRenderMatrixTest`, `I18nBundleParityTest` and `locales.parity.test.ts`.
- Every SMS costs one segment: covered by `SmsSegmentBudgetTest`.

- [ ] **Confirm nothing unrelated was committed**

Run: `git status --short`
Expected: only the pre-existing `docker/traefik/dynamic/local-dev.yml` and `keycloak/*.json` modifications remain unstaged. If anything else appears, it was missed by a task's `git add`.
