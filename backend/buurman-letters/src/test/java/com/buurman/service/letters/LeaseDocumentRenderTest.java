package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.LeaseKind;

/**
 * Real Thymeleaf engine and bundles over every shipped lease document (no PDF), for every {@link
 * LeaseDocumentRegistry} entry and enforced language: each translation (courtesy notice, no leaking
 * authoritative-language marker words) and each authoritative document. The clause set, required
 * clauses and marker words come from the registry; adding a country adds cases without test
 * changes. The per-language wording expectations below ({@link Lang}) are specific to the NL
 * residential documents and only apply to that entry; every other entry gets the language-neutral
 * assertions.
 */
@DisplayName("lease document render over the registry")
class LeaseDocumentRenderTest {

  /**
   * Per-language wording expectations of the NL residential translations. Substrings avoid HTML
   * entities and are matched against whitespace-normalized clause text.
   */
  record Lang(
      String code,
      Locale locale,
      String depositTerm,
      String articleRef,
      String inAdvance,
      String dueDay1,
      String dueDay5,
      String ownArticleWord) {
    Lang(
        String code,
        Locale locale,
        String depositTerm,
        String articleRef,
        String inAdvance,
        String dueDay1,
        String dueDay5) {
      this(code, locale, depositTerm, articleRef, inAdvance, dueDay1, dueDay5, "");
    }
  }

  static final List<Lang> LANGS =
      List.of(
          new Lang(
              "en",
              Locale.ENGLISH,
              "security deposit (waarborgsom)",
              "(see article <span>",
              "in advance",
              "no later than day <strong>1</strong>",
              "no later than day <strong>5</strong> of the month"),
          new Lang(
              "de",
              Locale.GERMAN,
              "Kaution (waarborgsom)",
              "(siehe Artikel <span>",
              "im Voraus",
              "am <strong>1</strong>. Tag",
              "am <strong>5</strong>. Tag"),
          new Lang(
              "fr",
              Locale.FRENCH,
              "dépôt de garantie (waarborgsom)",
              "(voir l’article <span>",
              "à l’avance",
              "le <strong>1</strong> du mois",
              "le <strong>5</strong> du mois"),
          new Lang(
              "es",
              Locale.of("es"),
              "fianza (waarborgsom)",
              "(véase el artículo <span>",
              "por adelantado",
              "el día <strong>1</strong> del mes",
              "el día <strong>5</strong> del mes"),
          new Lang(
              "pt",
              Locale.of("pt"),
              "caução (waarborgsom)",
              "(ver o artigo <span>",
              "antecipadamente",
              "o dia <strong>1</strong> do mês",
              "o dia <strong>5</strong> do mês"),
          new Lang(
              "it",
              Locale.ITALIAN,
              "deposito cauzionale (waarborgsom)",
              "(si veda l’articolo <span>",
              "in via anticipata",
              "il giorno <strong>1</strong> del mese",
              "il giorno <strong>5</strong> del mese"),
          new Lang(
              "sv",
              Locale.of("sv"),
              "deposition (waarborgsom)",
              "(se artikel <span>",
              "i förskott",
              "senast den <strong>1</strong> i den månad",
              "senast den <strong>5</strong> i den månad",
              "denna artikel"),
          new Lang(
              "da",
              Locale.of("da"),
              "depositum (waarborgsom)",
              "(se artikel <span>",
              "forud",
              "senest den <strong>1</strong>. i den måned",
              "senest den <strong>5</strong>. i den måned",
              "denne artikel"),
          new Lang(
              "nb",
              Locale.of("nb"),
              "depositum (waarborgsom)",
              "(se artikkel <span>",
              "forskuddsvis",
              "senest den <strong>1</strong>. i den måneden",
              "senest den <strong>5</strong>. i den måneden"),
          new Lang(
              "fi",
              Locale.of("fi"),
              "vakuuden (waarborgsom)",
              "(ks. <span>",
              "etukäteen",
              "viimeistään kunkin maksukauden alkamiskuukauden <strong>1</strong>. päivänä",
              "viimeistään kunkin maksukauden alkamiskuukauden <strong>5</strong>. päivänä"),
          new Lang(
              "el",
              Locale.of("el"),
              "εγγύηση (waarborgsom)",
              "(βλ. άρθρο <span>",
              "προκαταβολικά",
              "το αργότερο την <strong>1</strong>η ημέρα",
              "το αργότερο την <strong>5</strong>η ημέρα"),
          new Lang(
              "pl",
              Locale.of("pl"),
              "kaucję (waarborgsom)",
              "(zob. artykuł <span>",
              "z góry",
              "najpóźniej <strong>1</strong>. dnia miesiąca",
              "najpóźniej <strong>5</strong>. dnia miesiąca"));

  /** One registry entry rendered in one of its translation languages. */
  record Case(LeaseDocumentRegistry.Entry entry, String language) {
    Locale locale() {
      return Locale.forLanguageTag(language);
    }

    /** The NL residential wording expectations, absent for every other entry. */
    Optional<Lang> expectation() {
      if (!"NL/RESIDENTIAL".equals(entry.dbKey())) {
        return Optional.empty();
      }
      // a missing NL expectation must fail, not silently skip the NL wording assertions
      return Optional.of(
          LANGS.stream()
              .filter(l -> l.code().equals(language))
              .findFirst()
              .orElseThrow(
                  () -> new AssertionError("no NL wording expectation for language " + language)));
    }

    @Override
    public String toString() {
      return entry.key() + "/" + language;
    }
  }

  private static Stream<Case> translations() {
    return LeaseDocumentRegistry.ENTRIES.stream()
        .flatMap(e -> e.translations().stream().map(l -> new Case(e, l)));
  }

  private static Stream<LeaseDocumentRegistry.Entry> entries() {
    return LeaseDocumentRegistry.ENTRIES.stream();
  }

  private static final Pattern DATA_CLAUSE = Pattern.compile("data-clause=\"([a-z0-9-]+)\"");
  private static final Pattern DATA_REF = Pattern.compile("data-ref=\"([a-z0-9-]+)\"");

  /** Distinctive formatted values: found in the output only if a clause really prints them. */
  private static final String DEPOSIT = "EUR 7,654.32";

  private static final String END_DATE = "17 September 2031";

  private TemplateEngine engine;
  private MessageSource messages;

  @BeforeEach
  void setUp() {
    messages =
        DocumentTemplateSupport.messageSource(
            false,
            "classpath:messages/document-letter-chrome",
            "classpath:messages/document-lease-agreement");
    engine = DocumentTemplateSupport.templateEngine(messages, false);
  }

  private String render(
      LeaseDocumentRegistry.Entry entry,
      String language,
      boolean authoritative,
      List<String> orderedKeys,
      boolean withOptionalValues,
      Map<String, Object> overrides) {
    List<Map<String, Object>> clauses = new ArrayList<>();
    Map<String, Integer> refs = new HashMap<>();
    for (int i = 0; i < orderedKeys.size(); i++) {
      Map<String, Object> c = new HashMap<>();
      c.put("clauseKey", orderedKeys.get(i));
      c.put("title", "TITLE-" + orderedKeys.get(i));
      c.put("articleNumber", i + 1);
      clauses.add(c);
      refs.put(orderedKeys.get(i), i + 1);
    }
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", entry.documentPath(language));
    vars.put("authoritative", authoritative);
    vars.put("fallbackUsed", false);
    vars.put("languageUsed", language);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "3 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("propertyAddress", "Keizersgracht 1, 1015 AA Amsterdam");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord BV");
    vars.put("tenantNames", "J. Jansen, P. de Vries");
    vars.put("startDate", "1 November 2026");
    vars.put("contractTypeLabel", "Indefinite term");
    vars.put("rentAmount", "EUR 1,250.00");
    vars.put("paymentFrequency", "per month");
    vars.put("landlordNoticeDays", 90);
    vars.put("tenantNoticeDays", 30);
    vars.put("countryMetadata", null);
    vars.put("countryCode", entry.countryCode());
    vars.put("regionCode", null);
    vars.put("endDate", withOptionalValues ? END_DATE : null);
    vars.put("depositAmount", withOptionalValues ? DEPOSIT : null);
    vars.put("paymentDueDay", withOptionalValues ? 1 : null);
    vars.put("fixedTerm", withOptionalValues);
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    vars.putAll(overrides);
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx);
  }

  private String render(Case c, List<String> orderedKeys, boolean withOptionalValues) {
    return render(c.entry(), c.language(), false, orderedKeys, withOptionalValues, Map.of());
  }

  private String render(
      Case c, List<String> orderedKeys, boolean withOptionalValues, Map<String, Object> overrides) {
    return render(c.entry(), c.language(), false, orderedKeys, withOptionalValues, overrides);
  }

  private static List<String> renderedClauses(String html) {
    List<String> keys = new ArrayList<>();
    Matcher m = DATA_CLAUSE.matcher(html);
    while (m.find()) {
      keys.add(m.group(1));
    }
    return keys;
  }

  private static Set<String> referencedClauses(String html) {
    Set<String> keys = new LinkedHashSet<>();
    Matcher m = DATA_REF.matcher(html);
    while (m.find()) {
      keys.add(m.group(1));
    }
    return keys;
  }

  /** The document source of one language, to see which cross-references and values it uses. */
  private static String source(LeaseDocumentRegistry.Entry entry, String language) {
    try (var in =
        new ClassPathResource(
                DocumentTemplateSupport.TEMPLATE_PREFIX + entry.documentPath(language) + ".html")
            .getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void assertNoTemplateResidue(String html) {
    assertThat(html)
        .doesNotContain("${")
        .doesNotContain("#{")
        .doesNotContain("[[")
        .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})")
        .doesNotContain("??");
    assertThat(html)
        .as("raw bundle key rendered")
        .doesNotContainPattern(NlResidentialLeaseRenderTest.RAW_KEY);
  }

  private void assertClean(Case c, String html) {
    assertNoTemplateResidue(html);
    assertThat(html)
        .contains(messages.getMessage("lease.ref", new Object[] {"CON01TEST"}, c.locale()))
        .contains(messages.getMessage("lease.notice.courtesy", null, c.locale()));
    // NL wording check: sv and da legitimately use "artikel" in the cross-reference phrase and in
    // "denna/denne artikel" (this article); drop exactly those phrases first, any other "artikel"
    // is leakage of the Dutch text
    c.expectation()
        .ifPresent(
            lang -> {
              String withoutOwnWord = html.replace(lang.articleRef(), "");
              if (!lang.ownArticleWord().isEmpty()) {
                withoutOwnWord = withoutOwnWord.replace(lang.ownArticleWord(), "");
              }
              assertThat(withoutOwnWord).doesNotContain("artikel").doesNotContain("de huurder");
            });
  }

  /** Cross-references of the document appear exactly for the clauses that are included. */
  private void assertCrossReferences(
      LeaseDocumentRegistry.Entry entry, String language, String html, List<String> included) {
    Set<String> declared = referencedClauses(source(entry, language));
    Set<String> rendered = referencedClauses(html);
    assertThat(rendered)
        .as("%s/%s rendered cross-references", entry.key(), language)
        .isSubsetOf(included);
    assertThat(rendered)
        .as("%s/%s cross-references to included clauses", entry.key(), language)
        .containsExactlyInAnyOrderElementsOf(declared.stream().filter(included::contains).toList());
  }

  /**
   * The values the document prints appear in the render. Detected by what is rendered, not by
   * scanning the source: a deposit clause (key containing "deposit") must print the deposit amount,
   * a term/duration clause must print the end date; with optional clauses left out neither value
   * may appear.
   */
  private void assertValuesPrinted(LeaseDocumentRegistry.Entry entry, String html, boolean all) {
    assertThat(html.replaceAll("\\s+", " ")).contains("Example Landlord BV");
    boolean hasDeposit = entry.clauseKeys().stream().anyMatch(k -> k.contains("deposit"));
    boolean hasTerm =
        entry.clauseKeys().stream().anyMatch(k -> k.equals("term") || k.contains("duration"));
    if (all) {
      if (hasDeposit) {
        assertThat(html).as("%s deposit printed", entry.key()).contains(DEPOSIT);
      }
      if (hasTerm) {
        assertThat(html).as("%s end date printed", entry.key()).contains(END_DATE);
      }
    } else if (hasDeposit) {
      assertThat(html).as("%s optional deposit excluded", entry.key()).doesNotContain(DEPOSIT);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("translations")
  @DisplayName(
      "all clauses included: every clause renders in the language with the courtesy notice")
  void allIncluded(Case c) {
    List<String> all = c.entry().clauseKeys();
    for (boolean withOptionalValues : List.of(true, false)) {
      String html = render(c, all, withOptionalValues);
      assertThat(renderedClauses(html)).containsExactlyElementsOf(all);
      assertClean(c, html);
      assertThat(html.replaceAll("\\s+", " ")).contains("Example Landlord BV");
      c.expectation()
          .ifPresent(
              lang ->
                  assertThat(html.replaceAll("\\s+", " "))
                      .contains(lang.depositTerm())
                      .contains(lang.articleRef()));
    }
    String withValues = render(c, all, true);
    assertValuesPrinted(c.entry(), withValues, true);
    // the NL documents print both optional values, whatever the generic source check finds
    c.expectation().ifPresent(lang -> assertThat(withValues).contains(DEPOSIT).contains(END_DATE));
    assertCrossReferences(c.entry(), c.language(), withValues, all);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("translations")
  @DisplayName("only required clauses: optional bodies absent, references to them omitted")
  void requiredOnly(Case c) {
    List<String> required = c.entry().requiredClauseKeys();
    String html = render(c, required, true);
    assertThat(renderedClauses(html)).containsExactlyElementsOf(required);
    c.entry().clauseKeys().stream()
        .filter(k -> !required.contains(k))
        .forEach(
            k ->
                assertThat(html)
                    .doesNotContain("data-clause=\"" + k + "\"")
                    .doesNotContain("data-ref=\"" + k + "\""));
    assertCrossReferences(c.entry(), c.language(), html, required);
    assertClean(c, html);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("translations")
  @DisplayName("payment: a due day other than the 1st is not called payment in advance")
  void paymentDueDay(Case c) {
    List<String> all = c.entry().clauseKeys();
    String first = render(c, all, true, Map.of("paymentDueDay", 1));
    String fifth = render(c, all, true, Map.of("paymentDueDay", 5));
    assertClean(c, first);
    assertClean(c, fifth);
    c.expectation()
        .ifPresent(
            lang -> {
              assertThat(clauseBody(first, "payment"))
                  .contains(lang.inAdvance())
                  .contains(lang.dueDay1());
              assertThat(clauseBody(fifth, "payment"))
                  .contains(lang.dueDay5())
                  .doesNotContain(lang.inAdvance());
            });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("entries")
  @DisplayName("the authoritative document renders every clause cleanly, without courtesy notice")
  void authoritativeRenders(LeaseDocumentRegistry.Entry entry) {
    String language = entry.authoritativeLanguage();
    Locale locale = Locale.forLanguageTag(language);
    for (List<String> keys : List.of(entry.clauseKeys(), entry.requiredClauseKeys())) {
      String html = render(entry, language, true, keys, true, Map.of());
      assertThat(renderedClauses(html)).containsExactlyElementsOf(keys);
      assertNoTemplateResidue(html);
      assertThat(html)
          .contains(messages.getMessage("lease.ref", new Object[] {"CON01TEST"}, locale))
          .doesNotContain(messages.getMessage("lease.notice.courtesy", null, locale));
      assertValuesPrinted(entry, html, keys.equals(entry.clauseKeys()));
      assertCrossReferences(entry, language, html, keys);
    }
  }

  private static final Pattern PARENTHETICAL = Pattern.compile("\\([^()]*\\)");

  /**
   * Marker words left in the text outside parentheses (foreign terms are only allowed as
   * parenthetical glosses after the translated term).
   */
  static List<String> markersOutsideParentheses(String html, Pattern markers) {
    String text =
        html.replaceAll("(?s)<!--.*?-->", " ")
            .replaceAll("(?s)<style.*?</style>", " ")
            .replaceAll("(?s)<[^>]*>", " ")
            .replace("&nbsp;", " ");
    String previous;
    do {
      previous = text;
      text = PARENTHETICAL.matcher(text).replaceAll(" ");
    } while (!text.equals(previous));
    List<String> found = new ArrayList<>();
    Matcher m = markers.matcher(text.replaceAll("\\s+", " "));
    while (m.find()) {
      found.add(m.group());
    }
    return found;
  }

  /** The Dutch markers of the NL entry. */
  static List<String> dutchOutsideParentheses(String html) {
    return markersOutsideParentheses(
        html,
        LeaseDocumentRegistry.find("NL", LeaseKind.RESIDENTIAL)
            .flatMap(LeaseDocumentRegistry.Entry::foreignMarkerPattern)
            .orElseThrow());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("translations")
  @DisplayName("no foreign marker words of the entry outside parenthetical glosses")
  void noForeignMarkersOutsideParentheses(Case c) {
    c.entry()
        .foreignMarkerPattern()
        .ifPresent(
            markers -> {
              assertThat(
                      markersOutsideParentheses(render(c, c.entry().clauseKeys(), true), markers))
                  .isEmpty();
              assertThat(
                      markersOutsideParentheses(
                          render(c, c.entry().requiredClauseKeys(), false), markers))
                  .isEmpty();
            });
  }

  @Test
  @DisplayName(
      "Dutch marker check: bites on the Dutch text and on an injected term, spares glosses")
  void dutchMarkerCheckBites() throws IOException {
    String nl =
        new String(
            new ClassPathResource("templates/documents/lease-agreement/NL/residential/nl.html")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    assertThat(dutchOutsideParentheses(nl))
        .contains("de verhuurder", "tenzij", "overeenkomst", "deurwaardersexploot");
    assertThat(dutchOutsideParentheses("<p>Zie artikel 5 voor meer.</p>"))
        .containsExactly("Zie artikel");
    assertThat(
            dutchOutsideParentheses("<p>Uppsägning sker bij deurwaardersexploot eller brev.</p>"))
        .containsExactly("deurwaardersexploot");
    assertThat(
            dutchOutsideParentheses(
                "<p>Uppsägning sker genom delgivning (gerechtsdeurwaarder; deurwaardersexploot)"
                    + " eller brev.</p>"))
        .isEmpty();
  }

  @Test
  @DisplayName("an entry without marker words forbids nothing; markers are whole words, any case")
  void markerPatternSemantics() {
    LeaseDocumentRegistry.Entry none =
        new LeaseDocumentRegistry.Entry(
            "ZZ",
            LeaseKind.RESIDENTIAL,
            "en",
            List.of("en"),
            List.of(new LeaseDocumentRegistry.ClauseSpec("rent", true, false, 1)),
            List.of());
    assertThat(none.foreignMarkerPattern()).isEmpty();
    Pattern custom =
        new LeaseDocumentRegistry.Entry(
                "ZZ",
                LeaseKind.RESIDENTIAL,
                "en",
                List.of("en"),
                List.of(new LeaseDocumentRegistry.ClauseSpec("rent", true, false, 1)),
                List.of("the landlord", "unless"))
            .foreignMarkerPattern()
            .orElseThrow();
    assertThat(markersOutsideParentheses("<p>The Landlord pays UNLESS later.</p>", custom))
        .containsExactly("The Landlord", "UNLESS");
    assertThat(markersOutsideParentheses("<p>(the landlord) unlessness</p>", custom)).isEmpty();
  }

  private static String clauseBody(String html, String key) {
    int start = html.indexOf("data-clause=\"" + key + "\"");
    assertThat(start).as("clause %s rendered", key).isPositive();
    int end = html.indexOf("class=\"clause-title\"", start);
    return html.substring(start, end < 0 ? html.length() : end).replaceAll("\\s+", " ");
  }
}
