package com.buurman.service.letters;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.buurman.domain.LeaseKind;
import com.buurman.util.DocumentLanguages;

/**
 * The single list of shipped lease documents that every gate runs over: {@code
 * LeaseDocumentCatalogTest}, {@code LeaseDocumentFidelityTest}, {@code
 * TranslatedResidentialLeaseRenderTest}, {@code LeaseDocumentRegistryTest} and {@code
 * LeaseDocumentCatalogDatabaseIntegrationTest}.
 *
 * <h2>Adding a country</h2>
 *
 * Add ONE {@link Entry} to {@link #ENTRIES}, next to the files, the seed migration and the bundle
 * keys; no test code changes. Example, a German residential lease in German (authoritative) and
 * English:
 *
 * <pre>{@code
 * new Entry(
 *     "DE",
 *     LeaseKind.RESIDENTIAL,
 *     "de",
 *     List.of("de", "en"),
 *     List.of(
 *         new ClauseSpec("parties", true, true, 1),
 *         new ClauseSpec("premises", true, true, 2),
 *         new ClauseSpec("rent", true, false, 3)),
 *     List.of()) // foreign marker words to forbid in the translations, see Entry
 * }</pre>
 *
 * which makes the gates require, for {@code DE/residential}: the files {@code
 * templates/documents/lease-agreement/DE/residential/{de,en}.html} (exactly those, no other
 * language file), a {@code th:fragment="clause-<key>"} for exactly the listed keys in each, the
 * header markers, fidelity of {@code en.html} against {@code de.html}, the number-word lint on
 * {@code de.html}, a clean render of each language, the active {@code lease_clause_templates} rows
 * (country, kind, keys, required/pinned/order) and {@code lease.de.residential.<key>.title} and
 * {@code .summary} in all 13 bundles.
 *
 * <h2>Citation pattern (fidelity gate)</h2>
 *
 * The fidelity gate pairs each statute reference with the paragraph number that follows it within
 * three words of the same sentence and compares those pairs per element (so a swapped paragraph
 * digit is caught, a reordering inside one sentence is allowed). It needs only a regex for the
 * country's reference format, passed as the optional last {@code Entry} argument; without one the
 * digits are still compared per element but not paired. Examples, to be written with the sources of
 * the pack in hand:
 *
 * <ul>
 *   <li>NL (present): {@link #DUTCH_CITATION} {@code \d+:\d+[a-z]?} for "7:271 lid 2".
 *   <li>DE: {@code §\s*\d+[a-z]?} for "§ 556d Abs. 2" (and {@code Art\.\s*\d+} for "Art. 229").
 *   <li>FR: {@code art\.?\s*\d+(?:-\d+)?} for "art. 15-1, al. 2"; the paragraph digit after "al."
 *       is the digit the gate pairs.
 * </ul>
 *
 * Write the paragraph as a DIGIT after the reference in the same sentence in every language (see
 * {@code LeaseDocumentFidelityTest}); an alternation such as {@code §\s*\d+[a-z]?|Art\.\s*\d+} is
 * fine.
 *
 * <h2>Template variables for region branches</h2>
 *
 * Documents may branch on {@code countryCode} and the nullable {@code regionCode}. The region is
 * what the contract stores ({@code contracts.region_code}, VARCHAR(10), not case-validated by the
 * backend; the preview accepts 1-10 letters, digits or hyphens): in practice the catalog's code,
 * upper case, without a country prefix: ISO 3166-2 subdivision suffixes ("BY", "ON", "NY", "ENG",
 * "SCT", "VLG", "CAT", "QC") and city-level hyphen forms ("FR-PARIS", "NJ-NEWARK"; those exceed the
 * 10-character column when long). Region codes are not unique across countries ("NL" is
 * Newfoundland and Labrador in Canada), so branch on both: {@code th:if="${countryCode == 'CA' and
 * regionCode == 'NL'}"}.
 *
 * <h2>Countries without a national language (CZ)</h2>
 *
 * The locator never flags a document authoritative when the country has no national language, so
 * the English CZ document renders WITH the courtesy notice at runtime, even though the registry
 * entry names English as authoritative (header {@code translation: authoritative}, number-word
 * lint, fidelity source). {@code authoritativeRenders} forces {@code authoritative=true} and so
 * does not show the runtime notice; {@code LeaseDocumentLocatorTest} covers the runtime flag.
 */
public final class LeaseDocumentRegistry {

  private LeaseDocumentRegistry() {}

  /** The Dutch statute citation format: {@code 7:271}, {@code 7:261b}. */
  public static final Pattern DUTCH_CITATION = Pattern.compile("\\d+:\\d+[a-z]?");

  private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");
  private static final Pattern CLAUSE_KEY = Pattern.compile("[a-z0-9-]{1,64}");

  /** Mirror of one seeded {@code lease_clause_templates} row's structure. */
  public record ClauseSpec(String key, boolean required, boolean pinned, int sortOrder) {}

  /**
   * One (country, kind) with its documents.
   *
   * @param authoritativeLanguage the language of the authoritative document; every other enforced
   *     language is compared against it by the fidelity gate and must be {@code machine-drafted}
   * @param enforcedLanguages the languages that must exist, exactly (no other language file may sit
   *     in the directory); NL residential enforces all 13 supported document languages
   * @param clauses the clause set the seed migration inserts, in seed order
   * @param citationPattern the statute-reference format of the country, used by the fidelity gate
   *     to pair a reference with its paragraph digit ("§ 556 Abs. 2"), see the examples in the
   *     class Javadoc; empty = no pairing, digits are compared per element only
   * @param foreignMarkers words of a language the translations must NOT contain outside
   *     parenthetical glosses (typically the authoritative language leaking into a translation: for
   *     NL the Dutch markers); empty for an entry that has none yet
   */
  public record Entry(
      String countryCode,
      LeaseKind kind,
      String authoritativeLanguage,
      List<String> enforcedLanguages,
      List<ClauseSpec> clauses,
      List<String> foreignMarkers,
      Optional<Pattern> citationPattern) {

    /** An entry without a citation format: digits are compared per element only. */
    public Entry(
        String countryCode,
        LeaseKind kind,
        String authoritativeLanguage,
        List<String> enforcedLanguages,
        List<ClauseSpec> clauses,
        List<String> foreignMarkers) {
      this(
          countryCode,
          kind,
          authoritativeLanguage,
          enforcedLanguages,
          clauses,
          foreignMarkers,
          Optional.empty());
    }

    public Entry {
      if (!COUNTRY.matcher(countryCode).matches()) {
        throw new IllegalArgumentException(
            "country must be two upper-case letters: " + countryCode);
      }
      if (kind == LeaseKind.LEGACY) {
        throw new IllegalArgumentException("LEGACY has no per-language documents");
      }
      enforcedLanguages = List.copyOf(enforcedLanguages);
      if (enforcedLanguages.isEmpty()
          || !DocumentLanguages.ORDERED.containsAll(enforcedLanguages)
          || new HashSet<>(enforcedLanguages).size() != enforcedLanguages.size()) {
        throw new IllegalArgumentException(
            "enforced languages must be a non-empty, duplicate-free subset of "
                + DocumentLanguages.ORDERED
                + ": "
                + enforcedLanguages);
      }
      if (!enforcedLanguages.contains(authoritativeLanguage)) {
        throw new IllegalArgumentException(
            "authoritative language " + authoritativeLanguage + " must be enforced");
      }
      clauses = List.copyOf(clauses);
      Set<String> keys = new HashSet<>();
      for (ClauseSpec clause : clauses) {
        if (!CLAUSE_KEY.matcher(clause.key()).matches() || !keys.add(clause.key())) {
          throw new IllegalArgumentException("clause key invalid or repeated: " + clause.key());
        }
      }
      if (clauses.isEmpty()) {
        throw new IllegalArgumentException("an entry needs its clause set");
      }
      foreignMarkers = List.copyOf(foreignMarkers);
    }

    /** {@code NL/residential}: the document directory below {@code lease-agreement/}. */
    public String key() {
      return countryCode + "/" + kind.pathSegment();
    }

    /** {@code NL/RESIDENTIAL}: country and the {@code lease_kind} database value. */
    public String dbKey() {
      return countryCode + "/" + kind.name();
    }

    /** Classpath location of one language document, relative to {@code templates/documents/}. */
    public String documentPath(String language) {
      return "lease-agreement/" + key() + "/" + language;
    }

    /** The enforced languages other than the authoritative one, in enforced order. */
    public List<String> translations() {
      return enforcedLanguages.stream().filter(l -> !l.equals(authoritativeLanguage)).toList();
    }

    /** Clause keys in seed (sort) order. */
    public List<String> clauseKeys() {
      return clauses.stream().map(ClauseSpec::key).toList();
    }

    public List<String> requiredClauseKeys() {
      return clauses.stream().filter(ClauseSpec::required).map(ClauseSpec::key).toList();
    }

    /** Bundle key prefix of a clause: {@code lease.nl.residential.rent}. */
    public String i18nPrefix(String clauseKey) {
      return "lease."
          + countryCode.toLowerCase(Locale.ROOT)
          + "."
          + kind.pathSegment()
          + "."
          + clauseKey;
    }

    /**
     * Case-insensitive word-boundary pattern over {@link #foreignMarkers()}, or empty when the
     * entry has none.
     */
    public Optional<Pattern> foreignMarkerPattern() {
      if (foreignMarkers.isEmpty()) {
        return Optional.empty();
      }
      String alternatives = String.join("|", foreignMarkers.stream().map(Pattern::quote).toList());
      return Optional.of(
          Pattern.compile(
              "\\b(?:" + alternatives + ")\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }
  }

  /** Mirror of V092__seed_nl_residential_lease_clauses.sql. */
  private static final Entry NL_RESIDENTIAL =
      new Entry(
          "NL",
          LeaseKind.RESIDENTIAL,
          "nl",
          DocumentLanguages.ORDERED,
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-adjustment", false, false, 5),
              new ClauseSpec("service-costs", false, false, 6),
              new ClauseSpec("deposit", false, false, 7),
              new ClauseSpec("payment", true, false, 8),
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("energy-label", true, false, 12),
              new ClauseSpec("handover-inspection", false, false, 13),
              new ClauseSpec("termination", true, false, 14),
              new ClauseSpec("data-protection", false, false, 15),
              new ClauseSpec("disputes", false, false, 16)),
          List.of("de verhuurder", "tenzij", "overeenkomst", "zie artikel", "deurwaardersexploot"),
          Optional.of(DUTCH_CITATION));

  /** Every shipped (country, kind); add new entries here. */
  public static final List<Entry> ENTRIES = List.of(NL_RESIDENTIAL);

  /** The entry for a country and kind. */
  public static Optional<Entry> find(String countryCode, LeaseKind kind) {
    return ENTRIES.stream()
        .filter(e -> e.countryCode().equals(countryCode) && e.kind() == kind)
        .findFirst();
  }
}
