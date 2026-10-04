package com.buurman.service.letters;

import java.util.ArrayList;
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
 * LeaseDocumentCatalogTest}, {@code LeaseDocumentFidelityTest}, {@code LeaseDocumentRenderTest},
 * {@code LeaseDocumentRegistryTest} and {@code LeaseDocumentCatalogDatabaseIntegrationTest}.
 *
 * <h2>Adding a country</h2>
 *
 * Rule: a country pack edits ONLY inside its own {@code ===== <CC> =====} markers in this file and
 * in the 13 bundles; this keeps parallel packs merge-clean. Constants, citation pattern and foreign
 * markers go in the {@code <CC> entries (country pack)} block; the {@code entries.add(...)} lines
 * go in the {@code <CC> ENTRIES} block of {@code buildEntries()}.
 *
 * <p>Add ONE {@link Entry} to {@link #ENTRIES}, next to the files, the seed migration and the
 * bundle keys; no test code changes. Example, a German residential lease in German (authoritative)
 * and English:
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
 *   <li>FR (present): {@link #FRENCH_CITATION}, the number after "art."/"article(s)" with an
 *       optional code prefix, for "article 15-1, al. 2" and "article L. 145-4"; the paragraph digit
 *       after "al." is the digit the gate pairs.
 *   <li>ES (present): {@link #SPANISH_CITATION}, the number after "art."/"artículo(s)"/"article(s)"
 *       with an optional dotted paragraph, for "artículo 36.1 de la LAU" / "article 36.1 of the
 *       LAU"; the paragraph is part of the token.
 *   <li>PT (present): {@link #PORTUGUESE_CITATION}, the article digits after "artigo(s)"/"art."/
 *       "article(s)", for "artigo 1097.º, n.º 3, do Código Civil" / "article 1097, paragraph 3, of
 *       the Civil Code"; the ordinal sign glued to the number is skipped before the paragraph
 *       digit.
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

  /** The German statute citation format: {@code § 556}, {@code § 556d}, {@code § 573c}. */
  public static final Pattern GERMAN_CITATION = Pattern.compile("§\\s*\\d+[a-z]?");

  /** German marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> GERMAN_MARKERS =
      List.of(
          "der Vermieter", "der Mieter", "Mietverhältnis", "siehe Artikel", "unwirksam", "gemäß");

  /** Mirror of V093__seed_de_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry DE_RESIDENTIAL =
      new Entry(
          "DE",
          LeaseKind.RESIDENTIAL,
          "de",
          List.of("de", "en"),
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
              new ClauseSpec("minor-repairs", false, false, 12),
              new ClauseSpec("energy-certificate", false, false, 13),
              new ClauseSpec("registration", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("termination", true, false, 16),
              new ClauseSpec("data-protection", false, false, 17),
              new ClauseSpec("disputes", false, false, 18)),
          GERMAN_MARKERS,
          Optional.of(GERMAN_CITATION));

  /** Mirror of V093__seed_de_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry DE_COMMERCIAL =
      new Entry(
          "DE",
          LeaseKind.COMMERCIAL,
          "de",
          List.of("de", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("subletting", false, false, 13),
              new ClauseSpec("competition", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          GERMAN_MARKERS,
          Optional.of(GERMAN_CITATION));

  /**
   * The French statute citation format: the article number after "art." / "article(s)", with an
   * optional code prefix ("L. 145-4", "R. 145-35"): {@code article 22}, {@code art. 17-1}, {@code
   * article L. 145-40-2}. Only the number is the token, so "article" and "Article" pair alike.
   */
  public static final Pattern FRENCH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|articles?)\\s{1,20})(?:[LRD]\\.\\s{0,3})?\\d+(?:-\\d+)*",
          Pattern.CASE_INSENSITIVE);

  /** French marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> FRENCH_MARKERS =
      List.of("le bailleur", "le locataire", "le preneur", "voir l'article", "conformément");

  /** Mirror of V094__seed_fr_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry FR_RESIDENTIAL =
      new Entry(
          "FR",
          LeaseKind.RESIDENTIAL,
          "fr",
          List.of("fr", "en"),
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
              new ClauseSpec("works", false, false, 12),
              new ClauseSpec("diagnostics", true, false, 13),
              new ClauseSpec("insurance", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("termination", true, false, 16),
              new ClauseSpec("annexes", true, false, 17),
              new ClauseSpec("data-protection", false, false, 18),
              new ClauseSpec("disputes", false, false, 19)),
          FRENCH_MARKERS,
          Optional.of(FRENCH_CITATION));

  /** Mirror of V094__seed_fr_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry FR_COMMERCIAL =
      new Entry(
          "FR",
          LeaseKind.COMMERCIAL,
          "fr",
          List.of("fr", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", true, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("subletting", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("diagnostics", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("renewal", false, false, 18),
              new ClauseSpec("pre-emption", false, false, 19),
              new ClauseSpec("termination", true, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          FRENCH_MARKERS,
          Optional.of(FRENCH_CITATION));

  /**
   * The Spanish statute citation format: the article number after "art." / "artículo(s)" /
   * "article(s)", with an optional dotted paragraph: {@code artículo 9}, {@code artículo 36.1},
   * {@code article 52.1}, {@code artículo 1124}. Only the number is the token, so "artículo" and
   * "article" pair alike; a following "bis"/"ter" belongs to the token.
   */
  public static final Pattern SPANISH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|artículos?|articles?)\\s{1,20})\\d+(?:\\.\\d+)?(?:\\s+(?:bis|ter)\\b)?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Spanish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> SPANISH_MARKERS =
      List.of("el arrendador", "el arrendatario", "véase el artículo", "de conformidad con");

  /** Mirror of V095__seed_es_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry ES_RESIDENTIAL =
      new Entry(
          "ES",
          LeaseKind.RESIDENTIAL,
          "es",
          List.of("es", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-adjustment", false, false, 5),
              new ClauseSpec("service-costs", false, false, 6),
              new ClauseSpec("deposit", true, false, 7),
              new ClauseSpec("payment", true, false, 8),
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("pre-emption", false, false, 13),
              new ClauseSpec("subrogation", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("termination", true, false, 16),
              new ClauseSpec("notices", false, false, 17),
              new ClauseSpec("data-protection", false, false, 18),
              new ClauseSpec("disputes", false, false, 19)),
          SPANISH_MARKERS,
          Optional.of(SPANISH_CITATION));

  /** Mirror of V095__seed_es_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry ES_COMMERCIAL =
      new Entry(
          "ES",
          LeaseKind.COMMERCIAL,
          "es",
          List.of("es", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", true, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("pre-emption", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("goodwill-compensation", false, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          SPANISH_MARKERS,
          Optional.of(SPANISH_CITATION));

  /**
   * The Portuguese statute citation format: the article number after "artigo(s)" / "art." /
   * "article(s)": {@code artigo 1097.º, n.º 3} / {@code article 1097, paragraph 3}, {@code artigo
   * 1110.º-A} / {@code article 1110-A}. Only the digits are the token, so the ordinal sign and a
   * letter suffix ("1097.º", "1110.º-A") written in Portuguese and left out in English pair alike;
   * the "n.º" paragraph after the ordinal sign is the digit the gate pairs.
   */
  public static final Pattern PORTUGUESE_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|arts\\.|artigos?|articles?)\\s{1,20})\\d+",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Portuguese marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> PORTUGUESE_MARKERS =
      List.of("o senhorio", "o arrendatário", "ver o artigo", "nos termos do");

  /** Mirror of V096__seed_pt_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry PT_RESIDENTIAL =
      new Entry(
          "PT",
          LeaseKind.RESIDENTIAL,
          "pt",
          List.of("pt", "en"),
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
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("registration", false, false, 13),
              new ClauseSpec("pre-emption", false, false, 14),
              new ClauseSpec("succession", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("notices", false, false, 18),
              new ClauseSpec("guarantor", false, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          PORTUGUESE_MARKERS,
          Optional.of(PORTUGUESE_CITATION));

  /** Mirror of V096__seed_pt_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry PT_COMMERCIAL =
      new Entry(
          "PT",
          LeaseKind.COMMERCIAL,
          "pt",
          List.of("pt", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("pre-emption", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("registration", false, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("guarantor", false, false, 21),
              new ClauseSpec("data-protection", false, false, 22),
              new ClauseSpec("disputes", false, false, 23)),
          PORTUGUESE_MARKERS,
          Optional.of(PORTUGUESE_CITATION));

  // ===== AT entries (country pack) =====
  // Constants, citation pattern and foreign markers for AT go here, only inside these markers.

  /**
   * The Austrian statute citation format: {@code § 29 Abs. 1 Z 3 MRG}, {@code § 16b Abs. 2 MRG},
   * {@code § 1096 Abs. 1 ABGB}, {@code § 33 TP 5 Abs. 4 Z 1 GebG}. The token is the section number
   * with an optional letter suffix; the gate pairs it with the next bare number in the same
   * sentence, the "Abs." digit (English "para."), or for the fee act the "TP" (tariff item) digit,
   * so every translation keeps the German order "§ n Abs. m Z k" ("§ n para. m no. k").
   */
  public static final Pattern AUSTRIAN_CITATION = Pattern.compile("§\\s*\\d+[a-z]?");

  /** Austrian German marker words that must not leak into translations outside glosses. */
  private static final List<String> AUSTRIAN_MARKERS =
      List.of("der Vermieter", "der Mieter", "Mietverhältnis", "siehe Punkt", "unwirksam", "gemäß");

  /** Mirror of V099__seed_at_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry AT_RESIDENTIAL =
      new Entry(
          "AT",
          LeaseKind.RESIDENTIAL,
          "de",
          List.of("de", "en"),
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
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("registration", false, false, 13),
              new ClauseSpec("handover-inspection", false, false, 14),
              new ClauseSpec("termination", true, false, 15),
              new ClauseSpec("data-protection", false, false, 16),
              new ClauseSpec("disputes", false, false, 17)),
          AUSTRIAN_MARKERS,
          Optional.of(AUSTRIAN_CITATION));

  /** Mirror of V099__seed_at_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry AT_COMMERCIAL =
      new Entry(
          "AT",
          LeaseKind.COMMERCIAL,
          "de",
          List.of("de", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("subletting", false, false, 13),
              new ClauseSpec("insurance", false, false, 14),
              new ClauseSpec("energy-certificate", false, false, 15),
              new ClauseSpec("contract-fee", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          AUSTRIAN_MARKERS,
          Optional.of(AUSTRIAN_CITATION));

  // ===== end AT =====

  // ===== BE entries (country pack) =====
  // Constants, citation pattern and foreign markers for BE go here, only inside these markers.

  /**
   * The Belgian statute citation format, language-neutral across nl/fr/en: the article number after
   * "artikel(en)" / "article(s)" / "art.", with a Belgian slash or Latin suffix that belongs to the
   * token ({@code artikel 224/1}, {@code article 1728bis}, {@code artikel 51/1}). The paragraph is
   * written "§ n" after the reference in every language (nl "artikel 37, § 1", fr "article 37, §
   * 1er", en "article 37, § 1"); the gate pairs that digit (the "er" of the French "1er" is not a
   * digit). Paragraph numbers spelled as ordinals ("eerste lid") are not used in the documents.
   * After the plural ("artikelen" / "articles") the second number of a two-item list joined by
   * en/et/and or tot (en met)/à/to is a token too ("artikelen 224 en 224/1", "articles 17 à 19",
   * "articles 3 to 4bis"); longer comma lists keep their later numbers in the per-element digit
   * check.
   */
  public static final Pattern BELGIAN_CITATION =
      Pattern.compile(
          "(?:(?<=\\b(?:art\\.|artikel|artikelen|articles?)\\s{1,20})"
              + "|(?<=\\b(?:artikelen|articles)\\s{1,20}\\d{1,5}(?:/\\d{1,3})?"
              + "(?:bis|ter|quater|quinquies)?(?:\\s{0,3},\\s{0,3}§\\s{0,3}\\d{1,3}(?:er)?,?)?"
              + "\\s{1,5}(?:tot\\s{1,3}en\\s{1,3}met|tot|en|et|and|à|to)\\s{1,5}))"
              + "\\d+(?:/\\d+)?(?:bis|ter|quater|quinquies)?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Dutch marker words that must not leak into the fr/en translations outside glosses. */
  private static final List<String> BELGIAN_DUTCH_MARKERS =
      List.of("de verhuurder", "de huurder", "huurovereenkomst", "zie artikel", "overeenkomstig");

  /** Mirror of V097__seed_be_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry BE_RESIDENTIAL =
      new Entry(
          "BE",
          LeaseKind.RESIDENTIAL,
          "nl",
          List.of("nl", "fr", "en"),
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
              new ClauseSpec("insurance", false, false, 12),
              new ClauseSpec("energy-certificate", true, false, 13),
              new ClauseSpec("registration", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("transfer", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("annexes", true, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          BELGIAN_DUTCH_MARKERS,
          Optional.of(BELGIAN_CITATION));

  /** Mirror of V097__seed_be_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry BE_COMMERCIAL =
      new Entry(
          "BE",
          LeaseKind.COMMERCIAL,
          "nl",
          List.of("nl", "fr", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("insurance", false, false, 14),
              new ClauseSpec("energy-certificate", false, false, 15),
              new ClauseSpec("registration", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("renewal", false, false, 18),
              new ClauseSpec("transfer", false, false, 19),
              new ClauseSpec("termination", true, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          BELGIAN_DUTCH_MARKERS,
          Optional.of(BELGIAN_CITATION));

  // ===== end BE =====

  // ===== CA entries (country pack) =====
  // Constants, citation pattern and foreign markers for CA go here, only inside these markers.

  // ===== end CA =====

  // ===== CH entries (country pack) =====
  // Constants, citation pattern and foreign markers for CH go here, only inside these markers.

  // ===== end CH =====

  // ===== CZ entries (country pack) =====
  // Constants, citation pattern and foreign markers for CZ go here, only inside these markers.

  // ===== end CZ =====

  // ===== DK entries (country pack) =====
  // Constants, citation pattern and foreign markers for DK go here, only inside these markers.

  /**
   * The Danish statute citation format: the section sign and number, {@code § 59}, {@code § 175},
   * followed in the same sentence by the subsection digit ("§ 59, stk. 1" / "§ 59, subsection 1").
   * No letter suffix: Danish prose often follows a section number with the preposition "i" ("§ 7,
   * stk. 1, i lov om ..."), which must not become part of the token.
   */
  public static final Pattern DANISH_CITATION = Pattern.compile("§\\s*\\d+");

  /** Danish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> DANISH_MARKERS =
      List.of("udlejeren", "lejeren", "lejemålet", "lejeaftalen", "se artikel", "medmindre");

  /** Mirror of V100__seed_dk_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry DK_RESIDENTIAL =
      new Entry(
          "DK",
          LeaseKind.RESIDENTIAL,
          "da",
          List.of("da", "en"),
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
              new ClauseSpec("energy-label", false, false, 12),
              new ClauseSpec("handover-inspection", false, false, 13),
              new ClauseSpec("termination", true, false, 14),
              new ClauseSpec("data-protection", false, false, 15),
              new ClauseSpec("disputes", false, false, 16)),
          DANISH_MARKERS,
          Optional.of(DANISH_CITATION));

  /** Mirror of V100__seed_dk_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry DK_COMMERCIAL =
      new Entry(
          "DK",
          LeaseKind.COMMERCIAL,
          "da",
          List.of("da", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("subletting", false, false, 14),
              new ClauseSpec("energy-label", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("data-protection", false, false, 18),
              new ClauseSpec("disputes", false, false, 19)),
          DANISH_MARKERS,
          Optional.of(DANISH_CITATION));

  // ===== end DK =====

  // ===== FI entries (country pack) =====
  // Constants, citation pattern and foreign markers for FI go here, only inside these markers.

  /**
   * The Finnish statute citation format, the same token in all three FI languages: the section
   * number before the section sign in Finnish and Swedish ("AHVL 52 § 2 mom.", "13 a §") and after
   * "section" in English ("AHVL section 52, subsection 2", "section 13 a"). Only the number (with a
   * letter suffix) is the token, so the three forms pair alike; the "mom."/"subsection" digit that
   * follows within 3 words is the paragraph the gate pairs. FI documents write every citation in
   * parentheses with nothing but the subsection (and point) after the section, never the genitive
   * "52 §:n" (the colon would end the gate's sentence) and never section ranges.
   */
  public static final Pattern FINNISH_CITATION =
      Pattern.compile(
          "\\b\\d+(?:\\s?[a-z])?(?=\\s{0,3}§)|(?<=\\bsections?\\s{1,3})\\d+(?:\\s?[a-z])?\\b",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Finnish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> FINNISH_MARKERS =
      List.of(
          "vuokranantaja",
          "vuokranantajan",
          "vuokralainen",
          "vuokralaisen",
          "vuokrasopimus",
          "mitätön",
          "jollei");

  /** Mirror of V101__seed_fi_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry FI_RESIDENTIAL =
      new Entry(
          "FI",
          LeaseKind.RESIDENTIAL,
          "fi",
          List.of("fi", "sv", "en"),
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
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("handover-inspection", false, false, 13),
              new ClauseSpec("termination", true, false, 14),
              new ClauseSpec("notices", false, false, 15),
              new ClauseSpec("data-protection", false, false, 16),
              new ClauseSpec("disputes", false, false, 17)),
          FINNISH_MARKERS,
          Optional.of(FINNISH_CITATION));

  /** Mirror of V101__seed_fi_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry FI_COMMERCIAL =
      new Entry(
          "FI",
          LeaseKind.COMMERCIAL,
          "fi",
          List.of("fi", "sv", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("subletting", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("notices", false, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          FINNISH_MARKERS,
          Optional.of(FINNISH_CITATION));

  // ===== end FI =====

  // ===== GB entries (country pack) =====
  // Constants, citation pattern and foreign markers for GB go here, only inside these markers.

  /**
   * The United Kingdom statute citation format: the number after "section(s)" / "s." / "ss." /
   * "regulation(s)" / "reg." / "article(s)" / "paragraph(s)" / "ground(s)", with an optional
   * capital-letter suffix: {@code section 13}, {@code s.21}, {@code section 4A}, {@code regulation
   * 36}, {@code Ground 1A}. GB is English only (no translation is compared), so the pattern only
   * documents the format for the fidelity gate should a translation ever be added.
   */
  public static final Pattern UK_CITATION =
      Pattern.compile(
          "(?<=\\b(?:sections?|s\\.|ss\\.|regulations?|reg\\.|articles?|art\\.|paragraphs?"
              + "|para\\.|grounds?)\\s{0,20})\\d+[A-Z]*",
          Pattern.CASE_INSENSITIVE);

  /**
   * Mirror of V098__seed_gb_lease_clauses.sql (RESIDENTIAL rows). English only: no translation, so
   * no fidelity comparison and no foreign markers; nation branches on regionCode ENG/WLS/SCT/NIR.
   */
  private static final Entry GB_RESIDENTIAL =
      new Entry(
          "GB",
          LeaseKind.RESIDENTIAL,
          "en",
          List.of("en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-adjustment", false, false, 5),
              new ClauseSpec("service-costs", false, false, 6),
              new ClauseSpec("deposit", false, false, 7),
              new ClauseSpec("payment", true, false, 8),
              new ClauseSpec("fees", false, false, 9),
              new ClauseSpec("use", false, false, 10),
              new ClauseSpec("subletting", false, false, 11),
              new ClauseSpec("maintenance", true, false, 12),
              new ClauseSpec("safety", true, false, 13),
              new ClauseSpec("energy-certificate", false, false, 14),
              new ClauseSpec("registration", false, false, 15),
              new ClauseSpec("right-to-rent", false, false, 16),
              new ClauseSpec("statutory-information", true, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          List.of(),
          Optional.of(UK_CITATION));

  /** Mirror of V098__seed_gb_lease_clauses.sql (COMMERCIAL rows). English only. */
  private static final Entry GB_COMMERCIAL =
      new Entry(
          "GB",
          LeaseKind.COMMERCIAL,
          "en",
          List.of("en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("security-of-tenure", true, false, 5),
              new ClauseSpec("rent", true, false, 6),
              new ClauseSpec("rent-adjustment", false, false, 7),
              new ClauseSpec("service-costs", false, false, 8),
              new ClauseSpec("vat", false, false, 9),
              new ClauseSpec("deposit", false, false, 10),
              new ClauseSpec("payment", true, false, 11),
              new ClauseSpec("maintenance", false, false, 12),
              new ClauseSpec("alterations", false, false, 13),
              new ClauseSpec("assignment", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("break-option", false, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("guarantor", false, false, 21),
              new ClauseSpec("data-protection", false, false, 22),
              new ClauseSpec("disputes", false, false, 23)),
          List.of(),
          Optional.of(UK_CITATION));

  // ===== end GB =====

  // ===== GR entries (country pack) =====
  // Constants, citation pattern and foreign markers for GR go here, only inside these markers.

  /**
   * The Greek statute citation format, in Greek and English: the article digits after
   * "άρθρο/άρθρου/άρθρα/άρθρων" or "article(s)", and the law number "1703/1987" after "ν." / "π.δ."
   * or "Law" / "Decree": {@code άρθρο 13 παρ. 1 του ν. 4242/2014} and {@code article 13, paragraph
   * 1, of Law 4242/2014} both give "13 1" and "4242/2014". A Greek letter suffix ("612Α") is left
   * out of the token, so it pairs with the Latin "612A"; the law token ends the article's rest, so
   * the law number is never taken for its paragraph. Unicode-aware: Java's {@code \b} does not see
   * Greek letters.
   */
  public static final Pattern GREEK_CITATION =
      Pattern.compile(
          "(?<=(?<![\\p{L}\\p{N}])(?:άρθρ(?:ο|ου|α|ων)|articles?|ν\\.|π\\.\\s?δ\\.|Laws?|Decree)"
              + "\\s{1,20})\\d+(?:/\\d{4})?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /**
   * Greek marker words that must not leak into the English translations outside parenthetical
   * glosses. Not passed to the registry entry: {@link Entry#foreignMarkerPattern()} uses {@code
   * \b}, which does not match next to Greek letters, so {@code GrLeaseDocumentTest} checks them
   * with a Unicode-aware boundary.
   */
  public static final List<String> GREEK_MARKERS =
      List.of("ο εκμισθωτής", "ο μισθωτής", "μίσθιο", "βλ. άρθρο", "σύμφωνα με");

  /** Mirror of V102__seed_gr_lease_clauses.sql (RESIDENTIAL rows). */
  private static final Entry GR_RESIDENTIAL =
      new Entry(
          "GR",
          LeaseKind.RESIDENTIAL,
          "el",
          List.of("el", "en"),
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
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("registration", false, false, 13),
              new ClauseSpec("sale-of-premises", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("termination", true, false, 16),
              new ClauseSpec("data-protection", false, false, 17),
              new ClauseSpec("disputes", false, false, 18)),
          List.of(),
          Optional.of(GREEK_CITATION));

  /** Mirror of V102__seed_gr_lease_clauses.sql (COMMERCIAL rows). */
  private static final Entry GR_COMMERCIAL =
      new Entry(
          "GR",
          LeaseKind.COMMERCIAL,
          "el",
          List.of("el", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("vat", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("insurance", false, false, 14),
              new ClauseSpec("energy-certificate", false, false, 15),
              new ClauseSpec("registration", false, false, 16),
              new ClauseSpec("sale-of-premises", false, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          List.of(),
          Optional.of(GREEK_CITATION));

  // ===== end GR =====

  // ===== IE entries (country pack) =====
  // Constants, citation pattern and foreign markers for IE go here, only inside these markers.

  /**
   * The Irish statute citation format: the number after "section(s)" / "s." / "ss." /
   * "regulation(s)" / "reg." / "paragraph(s)" / "para." / "Part", with an optional capital-letter
   * suffix: {@code section 34}, {@code section 35B}, {@code s.19B}, {@code Part 4}. The subsection
   * in brackets ("section 34(1)(a)") belongs to the reference. Known limit: in a chained reference
   * ("sections 20(1) and 20B(2)") only the first number follows the keyword, so the later numbers
   * are not paired. IE is English only (no translation is compared), so the pattern only documents
   * the format for the fidelity gate should a translation ever be added.
   */
  public static final Pattern IRISH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:sections?|s\\.|ss\\.|regulations?|reg\\.|paragraphs?|para\\.|part)\\s{0,20})"
              + "\\d+[A-Z]*",
          Pattern.CASE_INSENSITIVE);

  /**
   * Mirror of V103__seed_ie_lease_clauses.sql (RESIDENTIAL rows). English only: no translation, so
   * no fidelity comparison and no foreign markers; no region branches (national rent limits since 1
   * March 2026).
   */
  private static final Entry IE_RESIDENTIAL =
      new Entry(
          "IE",
          LeaseKind.RESIDENTIAL,
          "en",
          List.of("en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-information", false, false, 5),
              new ClauseSpec("rent-adjustment", false, false, 6),
              new ClauseSpec("service-costs", false, false, 7),
              new ClauseSpec("deposit", false, false, 8),
              new ClauseSpec("payment", true, false, 9),
              new ClauseSpec("use", false, false, 10),
              new ClauseSpec("subletting", false, false, 11),
              new ClauseSpec("maintenance", false, false, 12),
              new ClauseSpec("energy-certificate", false, false, 13),
              new ClauseSpec("registration", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("notices", false, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          List.of(),
          Optional.of(IRISH_CITATION));

  /** Mirror of V103__seed_ie_lease_clauses.sql (COMMERCIAL rows). English only. */
  private static final Entry IE_COMMERCIAL =
      new Entry(
          "IE",
          LeaseKind.COMMERCIAL,
          "en",
          List.of("en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("renewal", true, false, 5),
              new ClauseSpec("rent", true, false, 6),
              new ClauseSpec("rent-adjustment", false, false, 7),
              new ClauseSpec("service-costs", false, false, 8),
              new ClauseSpec("vat", false, false, 9),
              new ClauseSpec("deposit", false, false, 10),
              new ClauseSpec("payment", true, false, 11),
              new ClauseSpec("maintenance", false, false, 12),
              new ClauseSpec("alterations", false, false, 13),
              new ClauseSpec("assignment", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("registration", false, false, 17),
              new ClauseSpec("break-option", false, false, 18),
              new ClauseSpec("handover-inspection", false, false, 19),
              new ClauseSpec("termination", true, false, 20),
              new ClauseSpec("notices", false, false, 21),
              new ClauseSpec("guarantor", false, false, 22),
              new ClauseSpec("data-protection", false, false, 23),
              new ClauseSpec("disputes", false, false, 24)),
          List.of(),
          Optional.of(IRISH_CITATION));

  // ===== end IE =====

  // ===== IT entries (country pack) =====
  // Constants, citation pattern and foreign markers for IT go here, only inside these markers.

  // ===== end IT =====

  // ===== LU entries (country pack) =====
  // Constants, citation pattern and foreign markers for LU go here, only inside these markers.

  // ===== end LU =====

  // ===== NL-COMMERCIAL entries (country pack) =====
  // Constants, citation pattern and foreign markers for NL-COMMERCIAL go here, only inside these
  // markers.

  // ===== end NL-COMMERCIAL =====

  // ===== NO entries (country pack) =====
  // Constants, citation pattern and foreign markers for NO go here, only inside these markers.

  // ===== end NO =====

  // ===== PL entries (country pack) =====
  // Constants, citation pattern and foreign markers for PL go here, only inside these markers.

  // ===== end PL =====

  // ===== SE entries (country pack) =====
  // Constants, citation pattern and foreign markers for SE go here, only inside these markers.

  // ===== end SE =====

  // ===== US entries (country pack) =====
  // Constants, citation pattern and foreign markers for US go here, only inside these markers.

  // ===== end US =====

  /** Every shipped (country, kind); a country pack adds its entries inside its own markers. */
  public static final List<Entry> ENTRIES = buildEntries();

  private static List<Entry> buildEntries() {
    List<Entry> entries = new ArrayList<>();
    entries.add(NL_RESIDENTIAL);
    entries.add(DE_RESIDENTIAL);
    entries.add(DE_COMMERCIAL);
    entries.add(FR_RESIDENTIAL);
    entries.add(FR_COMMERCIAL);
    entries.add(ES_RESIDENTIAL);
    entries.add(ES_COMMERCIAL);
    entries.add(PT_RESIDENTIAL);
    entries.add(PT_COMMERCIAL);

    // ===== AT ENTRIES (country pack) =====
    entries.add(AT_RESIDENTIAL);
    entries.add(AT_COMMERCIAL);

    // ===== end AT ENTRIES =====

    // ===== BE ENTRIES (country pack) =====
    entries.add(BE_RESIDENTIAL);
    entries.add(BE_COMMERCIAL);

    // ===== end BE ENTRIES =====

    // ===== CA ENTRIES (country pack) =====

    // ===== end CA ENTRIES =====

    // ===== CH ENTRIES (country pack) =====

    // ===== end CH ENTRIES =====

    // ===== CZ ENTRIES (country pack) =====

    // ===== end CZ ENTRIES =====

    // ===== DK ENTRIES (country pack) =====
    entries.add(DK_RESIDENTIAL);
    entries.add(DK_COMMERCIAL);

    // ===== end DK ENTRIES =====

    // ===== FI ENTRIES (country pack) =====
    entries.add(FI_RESIDENTIAL);
    entries.add(FI_COMMERCIAL);

    // ===== end FI ENTRIES =====

    // ===== GB ENTRIES (country pack) =====
    entries.add(GB_RESIDENTIAL);
    entries.add(GB_COMMERCIAL);

    // ===== end GB ENTRIES =====

    // ===== GR ENTRIES (country pack) =====
    entries.add(GR_RESIDENTIAL);
    entries.add(GR_COMMERCIAL);

    // ===== end GR ENTRIES =====

    // ===== IE ENTRIES (country pack) =====
    entries.add(IE_RESIDENTIAL);
    entries.add(IE_COMMERCIAL);

    // ===== end IE ENTRIES =====

    // ===== IT ENTRIES (country pack) =====

    // ===== end IT ENTRIES =====

    // ===== LU ENTRIES (country pack) =====

    // ===== end LU ENTRIES =====

    // ===== NL-COMMERCIAL ENTRIES (country pack) =====

    // ===== end NL-COMMERCIAL ENTRIES =====

    // ===== NO ENTRIES (country pack) =====

    // ===== end NO ENTRIES =====

    // ===== PL ENTRIES (country pack) =====

    // ===== end PL ENTRIES =====

    // ===== SE ENTRIES (country pack) =====

    // ===== end SE ENTRIES =====

    // ===== US ENTRIES (country pack) =====

    // ===== end US ENTRIES =====

    return List.copyOf(entries);
  }

  /** The entry for a country and kind. */
  public static Optional<Entry> find(String countryCode, LeaseKind kind) {
    return ENTRIES.stream()
        .filter(e -> e.countryCode().equals(countryCode) && e.kind() == kind)
        .findFirst();
  }
}
