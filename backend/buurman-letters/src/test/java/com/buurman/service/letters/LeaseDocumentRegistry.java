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
 * LeaseDocumentCatalogTest}, {@code LeaseDocumentFidelityTest}, {@code LeaseDocumentRenderTest},
 * {@code LeaseDocumentRegistryTest} and {@code LeaseDocumentCatalogDatabaseIntegrationTest}.
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

  /** Every shipped (country, kind); add new entries here. */
  public static final List<Entry> ENTRIES =
      List.of(
          NL_RESIDENTIAL,
          DE_RESIDENTIAL,
          DE_COMMERCIAL,
          FR_RESIDENTIAL,
          FR_COMMERCIAL,
          ES_RESIDENTIAL,
          ES_COMMERCIAL,
          PT_RESIDENTIAL,
          PT_COMMERCIAL);

  /** The entry for a country and kind. */
  public static Optional<Entry> find(String countryCode, LeaseKind kind) {
    return ENTRIES.stream()
        .filter(e -> e.countryCode().equals(countryCode) && e.kind() == kind)
        .findFirst();
  }
}
