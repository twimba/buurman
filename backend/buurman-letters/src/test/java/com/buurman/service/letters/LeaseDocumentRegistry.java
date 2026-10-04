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
 * the English CZ document renders WITH a notice at runtime (the no-national-version notice {@code
 * lease.notice.noNationalVersion}, not the courtesy one, which would point to a national version
 * that does not exist), even though the registry entry names English as authoritative (header
 * {@code translation: authoritative}, number-word lint, fidelity source). {@code
 * authoritativeRenders} forces {@code authoritative=true} and so does not show the runtime notice;
 * {@code LeaseDocumentLocatorTest} covers the runtime flag.
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
     * Case-insensitive whole-word pattern over {@link #foreignMarkers()}, or empty when the entry
     * has none. The word boundary is a Unicode-aware lookaround (no letter or digit of any script
     * before or after), not {@code \b}: Java's {@code \b} only sees ASCII word characters here, so
     * a marker starting or ending with a Greek, Cyrillic or accented Latin letter ("μίσθιο",
     * "déjà", "umową") would never match.
     */
    public Optional<Pattern> foreignMarkerPattern() {
      if (foreignMarkers.isEmpty()) {
        return Optional.empty();
      }
      String alternatives = String.join("|", foreignMarkers.stream().map(Pattern::quote).toList());
      return Optional.of(
          Pattern.compile(
              "(?<![\\p{L}\\p{N}])(?:" + alternatives + ")(?![\\p{L}\\p{N}])",
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }
  }

  /**
   * Mirror of the V092__seed_nl_residential_lease_clauses.sql section of
   * V082__lease_clause_library.sql.
   */
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

  /**
   * The German statute citation format: {@code § 556}, {@code § 556d}, {@code § 573c}. A paragraph
   * with a letter ("§ 556g Abs. 1a" / "§ 556g para. 1a") is a token of its own, so "1a" and "1b"
   * differ (a plain paragraph digit stays paired with its section).
   */
  public static final Pattern GERMAN_CITATION =
      Pattern.compile("§\\s*\\d+[a-z]?|(?<=\\b(?:Abs\\.|para\\.|paragraphs?)\\s{1,20})\\d+[a-z]+");

  /** German marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> GERMAN_MARKERS =
      List.of(
          "der Vermieter", "der Mieter", "Mietverhältnis", "siehe Artikel", "unwirksam", "gemäß");

  /**
   * Mirror of the V093__seed_de_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V093__seed_de_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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
   * article L. 145-40-2}. Only the number is the token, so "article" and "Article" pair alike. A
   * capital letter written after a space belongs to the token ({@code article 261 D} of the code
   * général des impôts), so "261 D" and "261 E" differ.
   */
  public static final Pattern FRENCH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|articles?)\\s{1,20})(?:[LRD]\\.\\s{0,3})?\\d+(?:-\\d+)*"
              + "(?:\\s(?-i:[A-Z])(?![\\p{L}\\p{N}]))?",
          Pattern.CASE_INSENSITIVE);

  /** French marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> FRENCH_MARKERS =
      List.of("le bailleur", "le locataire", "le preneur", "voir l'article", "conformément");

  /**
   * Mirror of the V094__seed_fr_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V094__seed_fr_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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
   * "article" pair alike; a following "bis"/"ter" belongs to the token. Every further dotted part
   * belongs to it too, a numeric one and a word one followed by a number ({@code artículo
   * 52.1.7.º}, {@code artículo 20.Uno.23.º} of the VAT Act), so "52.1.7" and "52.1.8" or
   * "20.Uno.23" and "20.Dos.23" differ.
   */
  public static final Pattern SPANISH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|artículos?|articles?)\\s{1,20})\\d+"
              + "(?:\\.(?:\\d+|\\p{L}+(?=\\.\\d)))*(?:\\s+(?:bis|ter)\\b)?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Spanish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> SPANISH_MARKERS =
      List.of("el arrendador", "el arrendatario", "véase el artículo", "de conformidad con");

  /**
   * Mirror of the V095__seed_es_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V095__seed_es_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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
   * the "n.º" paragraph after the ordinal sign is the digit the gate pairs. The letter suffix is a
   * token of its own, "-A" after the number with or without the ordinal sign ("1110.º-A" /
   * "1110-A", "15.º-A" / "15-A"), so "15-A" and "15-B" differ; the paragraph then pairs with the
   * suffix token.
   */
  public static final Pattern PORTUGUESE_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|arts\\.|artigos?|articles?)\\s{1,20})\\d+"
              + "|(?<=\\d|\\d\\.º)-(?-i:[A-Z])(?![\\p{L}\\p{N}])",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Portuguese marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> PORTUGUESE_MARKERS =
      List.of("o senhorio", "o arrendatário", "ver o artigo", "nos termos do");

  /**
   * Mirror of the V096__seed_pt_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V096__seed_pt_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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

  /**
   * Mirror of the V099__seed_at_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V099__seed_at_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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

  /**
   * Mirror of the V097__seed_be_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V097__seed_be_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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

  /**
   * The Canadian statute citation format: the section or article number after "section(s)" /
   * "article(s)" (English: "section 106, subsection 2, of the Residential Tenancies Act, 2006",
   * "article 1904 of the Civil Code of Quebec"; French: "article 106, paragraphe 2, de la Loi de
   * 2006 ...", "article 1904 du Code civil du Québec"). Decimal sections and articles ("12.1",
   * "47.0.1", "1974.1", "1978.2") and a letter suffix glued to the number stay in the token; the
   * subsection that follows within 3 words ("subsection 2" / "paragraphe 2") is the digit the gate
   * pairs.
   */
  public static final Pattern CANADIAN_CITATION =
      Pattern.compile(
          "(?<=\\b(?:sections?|articles?|art\\.|arts\\.)\\s{1,20})\\d+(?:\\.\\d+)*[a-z]?(?![\\w])",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** English marker words that must not leak into the French translation outside glosses. */
  private static final List<String> CANADIAN_ENGLISH_MARKERS =
      List.of(
          "the landlord", "the tenant", "the lessor", "the lessee", "see article", "pursuant to");

  /**
   * Mirror of the V111__seed_ca_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry CA_RESIDENTIAL =
      new Entry(
          "CA",
          LeaseKind.RESIDENTIAL,
          "en",
          List.of("en", "fr"),
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
              new ClauseSpec("handover-inspection", false, false, 12),
              new ClauseSpec("statutory-information", true, false, 13),
              new ClauseSpec("termination", true, false, 14),
              new ClauseSpec("notices", false, false, 15),
              new ClauseSpec("data-protection", false, false, 16),
              new ClauseSpec("disputes", false, false, 17)),
          CANADIAN_ENGLISH_MARKERS,
          Optional.of(CANADIAN_CITATION));

  /**
   * Mirror of the V111__seed_ca_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry CA_COMMERCIAL =
      new Entry(
          "CA",
          LeaseKind.COMMERCIAL,
          "en",
          List.of("en", "fr"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("rent", true, false, 5),
              new ClauseSpec("additional-rent", false, false, 6),
              new ClauseSpec("rent-adjustment", false, false, 7),
              new ClauseSpec("sales-tax", false, false, 8),
              new ClauseSpec("deposit", false, false, 9),
              new ClauseSpec("payment", true, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("alterations", false, false, 12),
              new ClauseSpec("assignment", false, false, 13),
              new ClauseSpec("insurance", false, false, 14),
              new ClauseSpec("indemnity", false, false, 15),
              new ClauseSpec("registration", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("notices", false, false, 19),
              new ClauseSpec("guarantor", false, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          CANADIAN_ENGLISH_MARKERS,
          Optional.of(CANADIAN_CITATION));

  // ===== end CA =====

  // ===== CH entries (country pack) =====
  // Constants, citation pattern and foreign markers for CH go here, only inside these markers.

  /**
   * The Swiss statute citation format in the 4 languages of the CH documents: the article number
   * with its letter suffix after "Art."/"art."/"Artikel"/"article(s)"/"articolo/i": de "Art. 271a
   * Abs. 1 OR", fr "art. 271a, al. 1, CO", it "art. 271a cpv. 1 CO", en "Article 271a paragraph 1
   * CO". The suffix letter belongs to the token (257a, 266l, 19a), so "Art. 257" and "Art. 257a"
   * never pair alike; the Abs./al./cpv./paragraph digit after it is the digit the gate pairs.
   */
  public static final Pattern SWISS_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|artikel|articles?|articol[oi])\\s{1,20})\\d+[a-z]?(?![\\da-z])",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Swiss German marker words that must not leak into the fr, it and en translations. */
  private static final List<String> SWISS_GERMAN_MARKERS =
      List.of(
          "der Vermieter", "der Mieter", "Mietverhältnis", "Mietzins", "siehe Artikel", "gemäss");

  /**
   * Mirror of the V110__seed_ch_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry CH_RESIDENTIAL =
      new Entry(
          "CH",
          LeaseKind.RESIDENTIAL,
          "de",
          List.of("de", "fr", "it", "en"),
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
              new ClauseSpec("handover-inspection", false, false, 12),
              new ClauseSpec("termination", true, false, 13),
              new ClauseSpec("data-protection", false, false, 14),
              new ClauseSpec("disputes", false, false, 15)),
          SWISS_GERMAN_MARKERS,
          Optional.of(SWISS_CITATION));

  /**
   * Mirror of the V110__seed_ch_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry CH_COMMERCIAL =
      new Entry(
          "CH",
          LeaseKind.COMMERCIAL,
          "de",
          List.of("de", "fr", "it", "en"),
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
              new ClauseSpec("assignment", false, false, 14),
              new ClauseSpec("retention-right", false, false, 15),
              new ClauseSpec("insurance", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          SWISS_GERMAN_MARKERS,
          Optional.of(SWISS_CITATION));

  // ===== end CH =====

  // ===== CZ entries (country pack) =====
  // Constants, citation pattern and foreign markers for CZ go here, only inside these markers.

  /**
   * The Czech statute citation format as written in the English CZ documents: the section number
   * after "section(s)", with an optional letter suffix and the paragraph in parentheses glued to
   * it: {@code section 2249(1)}, {@code section 2282a}, {@code section 7a(2)} (Czech "§ 2249 odst.
   * 1"). CZ is English only (no translation is compared), so the pattern only documents the format
   * for the fidelity gate should a translation ever be added.
   */
  public static final Pattern CZECH_CITATION =
      Pattern.compile(
          "(?<=\\bsections?\\s{1,20})\\d+[a-z]?(?:\\(\\d+\\))?", Pattern.CASE_INSENSITIVE);

  /**
   * Mirror of the V109__seed_cz_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows). English only: Czech is not a document language, so English is the only (and
   * authoritative) document; no foreign markers.
   */
  private static final Entry CZ_RESIDENTIAL =
      new Entry(
          "CZ",
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
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("energy-certificate", false, false, 12),
              new ClauseSpec("handover-inspection", false, false, 13),
              new ClauseSpec("succession", false, false, 14),
              new ClauseSpec("termination", true, false, 15),
              new ClauseSpec("notices", false, false, 16),
              new ClauseSpec("data-protection", false, false, 17),
              new ClauseSpec("disputes", false, false, 18)),
          List.of(),
          Optional.of(CZECH_CITATION));

  /**
   * Mirror of the V109__seed_cz_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows). English only.
   */
  private static final Entry CZ_COMMERCIAL =
      new Entry(
          "CZ",
          LeaseKind.COMMERCIAL,
          "en",
          List.of("en"),
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
              new ClauseSpec("assignment", false, false, 14),
              new ClauseSpec("insurance", false, false, 15),
              new ClauseSpec("energy-certificate", false, false, 16),
              new ClauseSpec("goodwill-compensation", false, false, 17),
              new ClauseSpec("handover-inspection", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          List.of(),
          Optional.of(CZECH_CITATION));

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

  /**
   * Mirror of the V100__seed_dk_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V100__seed_dk_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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

  /**
   * Mirror of the V101__seed_fi_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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

  /**
   * Mirror of the V101__seed_fi_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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
   * Mirror of the V098__seed_gb_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows). English only: no translation, so no fidelity comparison and no foreign
   * markers; nation branches on regionCode ENG/WLS/SCT/NIR.
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

  /**
   * Mirror of the V098__seed_gb_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows). English only.
   */
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
   * glosses ({@link Entry#foreignMarkerPattern()} uses a Unicode-aware word boundary, so Greek
   * letters are seen).
   */
  public static final List<String> GREEK_MARKERS =
      List.of("ο εκμισθωτής", "ο μισθωτής", "μίσθιο", "βλ. άρθρο", "σύμφωνα με");

  /**
   * Mirror of the V102__seed_gr_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
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
          GREEK_MARKERS,
          Optional.of(GREEK_CITATION));

  /**
   * Mirror of the V102__seed_gr_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
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
          GREEK_MARKERS,
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
   * Mirror of the V103__seed_ie_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows). English only: no translation, so no fidelity comparison and no foreign
   * markers; no region branches (national rent limits since 1 March 2026).
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

  /**
   * Mirror of the V103__seed_ie_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows). English only.
   */
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

  /**
   * The Italian statute citation format: the article number after "art." / "artt." /
   * "articolo/articoli" / "article(s)", with an optional Latin suffix: {@code art. 2, comma 1} /
   * {@code article 2, paragraph 1}, {@code art. 4-bis}, {@code art. 447-bis}. The "comma" /
   * "paragraph" digit that follows is the digit the gate pairs; a statute number such as "legge
   * 431/1998" is not a token (no "art." before it) and is compared as plain digits, written the
   * same way ("della legge 431/1998" / "of Law 431/1998") in both languages.
   */
  public static final Pattern ITALIAN_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|artt\\.|articol[oi]|articles?)\\s{1,20})\\d+(?:-(?:bis|ter|quater))?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Italian marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> ITALIAN_MARKERS =
      List.of(
          "il locatore", "il conduttore", "vedi l'articolo", "ai sensi dell'art", "della legge");

  /**
   * Mirror of the V104__seed_it_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry IT_RESIDENTIAL =
      new Entry(
          "IT",
          LeaseKind.RESIDENTIAL,
          "it",
          List.of("it", "en"),
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
              new ClauseSpec("energy-certificate", true, false, 12),
              new ClauseSpec("registration", false, false, 13),
              new ClauseSpec("succession", false, false, 14),
              new ClauseSpec("handover-inspection", false, false, 15),
              new ClauseSpec("termination", true, false, 16),
              new ClauseSpec("notices", false, false, 17),
              new ClauseSpec("guarantor", false, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          ITALIAN_MARKERS,
          Optional.of(ITALIAN_CITATION));

  /**
   * Mirror of the V104__seed_it_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry IT_COMMERCIAL =
      new Entry(
          "IT",
          LeaseKind.COMMERCIAL,
          "it",
          List.of("it", "en"),
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
              new ClauseSpec("goodwill-compensation", false, false, 15),
              new ClauseSpec("insurance", false, false, 16),
              new ClauseSpec("energy-certificate", true, false, 17),
              new ClauseSpec("registration", false, false, 18),
              new ClauseSpec("handover-inspection", false, false, 19),
              new ClauseSpec("termination", true, false, 20),
              new ClauseSpec("notices", false, false, 21),
              new ClauseSpec("guarantor", false, false, 22),
              new ClauseSpec("data-protection", false, false, 23),
              new ClauseSpec("disputes", false, false, 24)),
          ITALIAN_MARKERS,
          Optional.of(ITALIAN_CITATION));

  // ===== end IT =====

  // ===== LU entries (country pack) =====
  // Constants, citation pattern and foreign markers for LU go here, only inside these markers.

  /**
   * The Luxembourg statute citation format, language-neutral across fr/de/en: the article number
   * after "article(s)" / "art." / "Artikel(n)", with a hyphenated Code civil number or a Latin
   * suffix that belongs to the token ({@code article 1762-7}, {@code article 2quinquies}, {@code
   * Artikel 5 Absatz 2bis}). The paragraph is written as a digit after the reference in every
   * language (fr "article 12, paragraphe 3" / "article 1er, paragraphe 2", de "Artikel 12 Absatz
   * 3", en "article 12, paragraph 3"); the gate pairs that digit (the "er" of "1er" is skipped). An
   * alinéa is "alinéa 2" / "Unterabsatz 2" / "subparagraph 2" after the paragraph. The statute is
   * named "loi modifiée du 21 septembre 2006" / "geänderten Gesetzes vom 21. September 2006" /
   * "amended Law of 21 September 2006" so that its date never sits within 3 words of an article. A
   * paragraph with a Latin suffix ("paragraphe 2bis" / "Absatz 2bis" / "paragraph 2bis") is a token
   * of its own, so "2bis" and "2ter" differ (a plain paragraph digit stays paired with its
   * article).
   */
  public static final Pattern LUXEMBOURG_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|articles?|artikel|artikeln)\\s{1,20})\\d+(?:-\\d+)*"
              + "(?:bis|ter|quater|quinquies|sexies)?"
              + "|(?<=\\b(?:paragraphes?|paragraphs?|absatz|abs\\.)\\s{1,20})\\d+"
              + "(?:bis|ter|quater|quinquies|sexies)",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** French marker words that must not leak into the de/en translations outside glosses. */
  private static final List<String> LUXEMBOURG_FRENCH_MARKERS =
      List.of("le bailleur", "le locataire", "le preneur", "voir l'article", "conformément");

  /**
   * Mirror of the V105__seed_lu_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry LU_RESIDENTIAL =
      new Entry(
          "LU",
          LeaseKind.RESIDENTIAL,
          "fr",
          List.of("fr", "de", "en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-adjustment", false, false, 5),
              new ClauseSpec("service-costs", true, false, 6),
              new ClauseSpec("deposit", false, false, 7),
              new ClauseSpec("payment", true, false, 8),
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("shared-tenancy", false, false, 11),
              new ClauseSpec("maintenance", false, false, 12),
              new ClauseSpec("energy-certificate", false, false, 13),
              new ClauseSpec("pre-emption", false, false, 14),
              new ClauseSpec("succession", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("data-protection", false, false, 18),
              new ClauseSpec("disputes", false, false, 19)),
          LUXEMBOURG_FRENCH_MARKERS,
          Optional.of(LUXEMBOURG_CITATION));

  /**
   * Mirror of the V105__seed_lu_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry LU_COMMERCIAL =
      new Entry(
          "LU",
          LeaseKind.COMMERCIAL,
          "fr",
          List.of("fr", "de", "en"),
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
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("renewal", false, false, 17),
              new ClauseSpec("pre-emption", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          LUXEMBOURG_FRENCH_MARKERS,
          Optional.of(LUXEMBOURG_CITATION));

  // ===== end LU =====

  // ===== NL-COMMERCIAL entries (country pack) =====
  // Constants, citation pattern and foreign markers for NL-COMMERCIAL go here, only inside these
  // markers.

  /**
   * Mirror of the V113__seed_nl_commercial_lease_clauses.sql section of
   * V082__lease_clause_library.sql: huur van bedrijfsruimte, nl authoritative and an English
   * courtesy translation (the other app languages fall back to nl). Same citation format and Dutch
   * markers as NL residential.
   */
  private static final Entry NL_COMMERCIAL =
      new Entry(
          "NL",
          LeaseKind.COMMERCIAL,
          "nl",
          List.of("nl", "en"),
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
              new ClauseSpec("energy-label", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("data-protection", false, false, 18),
              new ClauseSpec("disputes", false, false, 19)),
          List.of("de verhuurder", "tenzij", "overeenkomst", "zie artikel", "deurwaardersexploot"),
          Optional.of(DUTCH_CITATION));

  // ===== end NL-COMMERCIAL =====

  // ===== NO entries (country pack) =====
  // Constants, citation pattern and foreign markers for NO go here, only inside these markers.

  /**
   * The Norwegian statute citation format: the section sign with chapter and section, {@code §
   * 9-3}, {@code § 3-5}, {@code § 13-2}, or a plain section of a regulation, {@code § 6}. Norwegian
   * writes the paragraph (ledd) as an ordinal word ("§ 9-3 andre ledd"), which the fidelity gate
   * cannot pair with the English "paragraph 2"; the NO documents therefore write the ledd as a
   * digit ordinal in Norwegian, "§ 9-3 2. ledd", and as "§ 9-3, paragraph 2" in English, so the
   * gate pairs (9-3, 2) in both. The letter of a letter section belongs to the token ("§ 9-3 a"),
   * so "§ 9-3 a" and "§ 9-3 b" differ; a single letter followed by a word ("§ 9-3 i husleieloven",
   * "§ 9-3 a landlord") is not a suffix.
   */
  public static final Pattern NORWEGIAN_CITATION =
      Pattern.compile("§\\s*\\d+(?:-\\d+)?(?:\\s?[a-z](?![\\p{L}\\p{N}])(?!\\s+\\p{L}))?");

  /** Norwegian marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> NORWEGIAN_MARKERS =
      List.of("utleieren", "leieren", "leieavtalen", "husrommet", "med mindre");

  /**
   * Mirror of the V106__seed_no_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry NO_RESIDENTIAL =
      new Entry(
          "NO",
          LeaseKind.RESIDENTIAL,
          "nb",
          List.of("nb", "en"),
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
              new ClauseSpec("enforcement", false, false, 15),
              new ClauseSpec("data-protection", false, false, 16),
              new ClauseSpec("disputes", false, false, 17)),
          NORWEGIAN_MARKERS,
          Optional.of(NORWEGIAN_CITATION));

  /**
   * Mirror of the V106__seed_no_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry NO_COMMERCIAL =
      new Entry(
          "NO",
          LeaseKind.COMMERCIAL,
          "nb",
          List.of("nb", "en"),
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
              new ClauseSpec("energy-label", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("enforcement", false, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          NORWEGIAN_MARKERS,
          Optional.of(NORWEGIAN_CITATION));

  // ===== end NO =====

  // ===== PL entries (country pack) =====
  // Constants, citation pattern and foreign markers for PL go here, only inside these markers.

  /**
   * The Polish statute citation format. Two kinds of token, each WITH its letter or superscript
   * suffix, so "art. 6b" vs "Article 6c", "688¹" vs "688²", "8a" vs "8" or "ust. 1b" vs "paragraph
   * 1" differ:
   *
   * <ul>
   *   <li>the article after "art." / "article(s)": {@code art. 688¹ § 1 k.c.} / {@code Article 688¹
   *       § 1 of the Civil Code} give "688¹ 1"; {@code art. 6 ust. 1 u.o.p.l.} / {@code Article 6,
   *       paragraph 1, of the Tenant Protection Act} give "6 1"; {@code art. 8a ust. 4} / {@code
   *       Article 8a, paragraph 4} give "8a 4" (the plain paragraph digit after "§" / "ust." /
   *       "paragraph" is paired by the gate);
   *   <li>a paragraph that carries a letter, after "ust." / "paragraph(s)": {@code art. 9 ust. 1b}
   *       / {@code Article 9, paragraph 1b} give "9" and "1b" (a plain paragraph digit is not a
   *       token of its own, it stays paired with its article).
   * </ul>
   *
   * The superscript is written as "¹" / "²" / "³" in every language, never "^1" or "(1)". "art." /
   * "Article" is repeated before every article number in both languages ("art. 19a i art. 19f" /
   * "Article 19a and Article 19f").
   */
  public static final Pattern POLISH_CITATION =
      Pattern.compile(
          "(?<=\\b(?:art\\.|articles?)\\s{1,20})\\d+[a-z]*[¹²³]?"
              + "|(?<=\\b(?:ust\\.|paragraphs?)\\s{1,20})\\d+[a-z]+",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Polish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> POLISH_MARKERS =
      List.of("wynajmujący", "najemca", "najemcy", "zgodnie z", "zob. artykuł", "niniejsza umowa");

  /**
   * Mirror of the V107__seed_pl_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry PL_RESIDENTIAL =
      new Entry(
          "PL",
          LeaseKind.RESIDENTIAL,
          "pl",
          List.of("pl", "en"),
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
              new ClauseSpec("succession", false, false, 14),
              new ClauseSpec("termination", true, false, 15),
              new ClauseSpec("notices", false, false, 16),
              new ClauseSpec("data-protection", false, false, 17),
              new ClauseSpec("disputes", false, false, 18)),
          POLISH_MARKERS,
          Optional.of(POLISH_CITATION));

  /**
   * Mirror of the V107__seed_pl_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry PL_COMMERCIAL =
      new Entry(
          "PL",
          LeaseKind.COMMERCIAL,
          "pl",
          List.of("pl", "en"),
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
              new ClauseSpec("enforcement-submission", false, false, 16),
              new ClauseSpec("handover-inspection", false, false, 17),
              new ClauseSpec("termination", true, false, 18),
              new ClauseSpec("notices", false, false, 19),
              new ClauseSpec("data-protection", false, false, 20),
              new ClauseSpec("disputes", false, false, 21)),
          POLISH_MARKERS,
          Optional.of(POLISH_CITATION));

  // ===== end PL =====

  // ===== SE entries (country pack) =====
  // Constants, citation pattern and foreign markers for SE go here, only inside these markers.

  /**
   * The Swedish statute citation format, the same token in Swedish and English: the section number
   * before the section sign in Swedish ("12 kap. 46 § 1 st. JB", "12 kap. 18 i §", "13 § lagen
   * (2006:985)") and after "section" in English ("Land Code, Chapter 12, section 46, paragraph 1").
   * Only the section number (with a letter suffix, "45 a" -> "45a") is the token; the chapter
   * number before it is a bare number in both languages, and the stycke/"paragraph" digit that
   * follows within 3 words is the paragraph the gate pairs. SE documents therefore write the
   * chapter BEFORE the section in English too, give every section its own "§" / "section" (no
   * "45-46 §§", no "34 och 35 §§", no "sections 34 and 35"), and write the stycke as a digit ("1
   * st.", never "första stycket").
   */
  public static final Pattern SWEDISH_CITATION =
      Pattern.compile(
          "\\b\\d+(?:\\s?[a-z])?(?=\\s{0,3}§)|(?<=\\bsection\\s{1,3})\\d+(?:\\s?[a-z])?\\b",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** Swedish marker words that must not leak into translations outside parenthetical glosses. */
  private static final List<String> SWEDISH_MARKERS =
      List.of("hyresvärden", "hyresgästen", "hyresavtalet", "lägenheten", "se artikel", "enligt");

  /**
   * Mirror of the V108__seed_se_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows).
   */
  private static final Entry SE_RESIDENTIAL =
      new Entry(
          "SE",
          LeaseKind.RESIDENTIAL,
          "sv",
          List.of("sv", "en"),
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
          SWEDISH_MARKERS,
          Optional.of(SWEDISH_CITATION));

  /**
   * Mirror of the V108__seed_se_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows).
   */
  private static final Entry SE_COMMERCIAL =
      new Entry(
          "SE",
          LeaseKind.COMMERCIAL,
          "sv",
          List.of("sv", "en"),
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
              new ClauseSpec("renewal", false, false, 18),
              new ClauseSpec("termination", true, false, 19),
              new ClauseSpec("notices", false, false, 20),
              new ClauseSpec("data-protection", false, false, 21),
              new ClauseSpec("disputes", false, false, 22)),
          SWEDISH_MARKERS,
          Optional.of(SWEDISH_CITATION));

  // ===== end SE =====

  // ===== US entries (country pack) =====
  // Constants, citation pattern and foreign markers for US go here, only inside these markers.

  /**
   * The United States statute citation format: the section number after "§" / "§§" / "section(s)"
   * or after the code abbreviations "ORS", "RCW" and "N.J.S.A.", keeping every dotted, colon or
   * hyphen segment and letter suffix in the token: {@code § 1950.5}, {@code § 2079.10a}, {@code §
   * 7-108}, {@code § 235-b}, {@code § 504B.178}, {@code § 4852d}, {@code section 15B}, {@code ORS
   * 90.323}, {@code RCW 59.18.280}, {@code N.J.S.A. 46:8-21.2}. US is English only (no translation
   * is compared), so the pattern documents the format for the fidelity gate should a translation
   * ever be added.
   */
  public static final Pattern US_CITATION =
      Pattern.compile(
          "(?<=(?:§|§§|\\bsections?|\\bORS|\\bRCW|\\bN\\.J\\.S\\.A\\.)\\s{0,3})"
              + "\\d+[A-Za-z]?(?:[.:-]\\d+[A-Za-z]?)*(?:-[A-Za-z](?![A-Za-z]))?",
          Pattern.CASE_INSENSITIVE);

  /**
   * Mirror of the V112__seed_us_lease_clauses.sql section of V082__lease_clause_library.sql
   * (RESIDENTIAL rows). English only: no translation, so no fidelity comparison and no foreign
   * markers; state branches on the first 2 letters of regionCode (CA, DC, MA, MD, ME, MN, NJ, NY,
   * OR, WA) with a general text for any other state.
   */
  private static final Entry US_RESIDENTIAL =
      new Entry(
          "US",
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
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("maintenance", true, false, 11),
              new ClauseSpec("entry", false, false, 12),
              new ClauseSpec("disclosures", true, false, 13),
              new ClauseSpec("safety", false, false, 14),
              new ClauseSpec("fair-housing", false, false, 15),
              new ClauseSpec("handover-inspection", false, false, 16),
              new ClauseSpec("termination", true, false, 17),
              new ClauseSpec("notices", false, false, 18),
              new ClauseSpec("data-protection", false, false, 19),
              new ClauseSpec("disputes", false, false, 20)),
          List.of(),
          Optional.of(US_CITATION));

  /**
   * Mirror of the V112__seed_us_lease_clauses.sql section of V082__lease_clause_library.sql
   * (COMMERCIAL rows). English only.
   */
  private static final Entry US_COMMERCIAL =
      new Entry(
          "US",
          LeaseKind.COMMERCIAL,
          "en",
          List.of("en"),
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("permitted-use", true, false, 3),
              new ClauseSpec("term", true, false, 4),
              new ClauseSpec("renewal-option", false, false, 5),
              new ClauseSpec("rent", true, false, 6),
              new ClauseSpec("rent-adjustment", false, false, 7),
              new ClauseSpec("service-costs", false, false, 8),
              new ClauseSpec("sales-tax", false, false, 9),
              new ClauseSpec("deposit", false, false, 10),
              new ClauseSpec("payment", true, false, 11),
              new ClauseSpec("maintenance", false, false, 12),
              new ClauseSpec("alterations", false, false, 13),
              new ClauseSpec("accessibility", false, false, 14),
              new ClauseSpec("assignment", false, false, 15),
              new ClauseSpec("insurance", false, false, 16),
              new ClauseSpec("subordination", false, false, 17),
              new ClauseSpec("holdover", false, false, 18),
              new ClauseSpec("handover-inspection", false, false, 19),
              new ClauseSpec("termination", true, false, 20),
              new ClauseSpec("notices", false, false, 21),
              new ClauseSpec("guarantor", false, false, 22),
              new ClauseSpec("recording", false, false, 23),
              new ClauseSpec("data-protection", false, false, 24),
              new ClauseSpec("disputes", false, false, 25)),
          List.of(),
          Optional.of(US_CITATION));

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
    entries.add(CA_RESIDENTIAL);
    entries.add(CA_COMMERCIAL);

    // ===== end CA ENTRIES =====

    // ===== CH ENTRIES (country pack) =====
    entries.add(CH_RESIDENTIAL);
    entries.add(CH_COMMERCIAL);

    // ===== end CH ENTRIES =====

    // ===== CZ ENTRIES (country pack) =====
    entries.add(CZ_RESIDENTIAL);
    entries.add(CZ_COMMERCIAL);

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
    entries.add(IT_RESIDENTIAL);
    entries.add(IT_COMMERCIAL);

    // ===== end IT ENTRIES =====

    // ===== LU ENTRIES (country pack) =====
    entries.add(LU_RESIDENTIAL);
    entries.add(LU_COMMERCIAL);

    // ===== end LU ENTRIES =====

    // ===== NL-COMMERCIAL ENTRIES (country pack) =====
    entries.add(NL_COMMERCIAL);

    // ===== end NL-COMMERCIAL ENTRIES =====

    // ===== NO ENTRIES (country pack) =====
    entries.add(NO_RESIDENTIAL);
    entries.add(NO_COMMERCIAL);

    // ===== end NO ENTRIES =====

    // ===== PL ENTRIES (country pack) =====
    entries.add(PL_RESIDENTIAL);
    entries.add(PL_COMMERCIAL);

    // ===== end PL ENTRIES =====

    // ===== SE ENTRIES (country pack) =====
    entries.add(SE_RESIDENTIAL);
    entries.add(SE_COMMERCIAL);

    // ===== end SE ENTRIES =====

    // ===== US ENTRIES (country pack) =====
    entries.add(US_RESIDENTIAL);
    entries.add(US_COMMERCIAL);

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
