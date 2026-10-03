package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/** Real Thymeleaf engine and bundles over each courtesy translation (no PDF). */
@DisplayName("translated NL residential lease document render")
class TranslatedResidentialLeaseRenderTest {

  /**
   * Per-language expectations. Substrings avoid HTML entities and are matched against
   * whitespace-normalized clause text.
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
              "viimeistään kunkin maksukauden alkamiskuukauden <strong>5</strong>. päivänä"));

  private static Stream<Lang> languages() {
    return LANGS.stream();
  }

  private static final List<String> ALL =
      List.of(
          "parties",
          "premises",
          "term",
          "rent",
          "rent-adjustment",
          "service-costs",
          "deposit",
          "payment",
          "use",
          "subletting",
          "maintenance",
          "energy-label",
          "handover-inspection",
          "termination",
          "data-protection",
          "disputes");

  private static final List<String> REQUIRED =
      List.of("parties", "premises", "term", "rent", "payment", "energy-label", "termination");

  private static final Pattern DATA_CLAUSE = Pattern.compile("data-clause=\"([a-z0-9-]+)\"");
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

  private String render(Lang lang, List<String> orderedKeys, boolean withOptionalValues) {
    return render(lang, orderedKeys, withOptionalValues, Map.of());
  }

  private String render(
      Lang lang,
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
    vars.put("clauseSource", "lease-agreement/NL/residential/" + lang.code());
    vars.put("authoritative", false);
    vars.put("fallbackUsed", false);
    vars.put("languageUsed", lang.code());
    vars.put("requestedLang", lang.code());
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
    vars.put("endDate", withOptionalValues ? "31 October 2028" : null);
    vars.put("depositAmount", withOptionalValues ? "EUR 2,500.00" : null);
    vars.put("paymentDueDay", withOptionalValues ? 1 : null);
    vars.put("fixedTerm", withOptionalValues);
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    vars.putAll(overrides);
    Context ctx = new Context(lang.locale());
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx);
  }

  private static List<String> renderedClauses(String html) {
    List<String> keys = new ArrayList<>();
    Matcher m = DATA_CLAUSE.matcher(html);
    while (m.find()) {
      keys.add(m.group(1));
    }
    return keys;
  }

  private void assertClean(Lang lang, String html) {
    assertThat(html)
        .doesNotContain("${")
        .doesNotContain("#{")
        .doesNotContain("[[")
        .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})")
        .doesNotContain("??")
        .contains(messages.getMessage("lease.ref", new Object[] {"CON01TEST"}, lang.locale()))
        .contains(messages.getMessage("lease.notice.courtesy", null, lang.locale()));
    assertThat(html)
        .as("raw bundle key rendered")
        .doesNotContainPattern(NlResidentialLeaseRenderTest.RAW_KEY);
    // sv and da legitimately use "artikel" in the cross-reference phrase and in "denna/denne
    // artikel" (this article); drop exactly those phrases first, any other "artikel" is leakage
    String withoutOwnWord = html.replace(lang.articleRef(), "");
    if (!lang.ownArticleWord().isEmpty()) {
      withoutOwnWord = withoutOwnWord.replace(lang.ownArticleWord(), "");
    }
    assertThat(withoutOwnWord).doesNotContain("artikel").doesNotContain("de huurder");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("languages")
  @DisplayName(
      "all clauses included: every clause renders in the language with the courtesy notice")
  void allIncluded(Lang lang) {
    for (boolean withOptionalValues : List.of(true, false)) {
      String html = render(lang, ALL, withOptionalValues);
      assertThat(renderedClauses(html)).containsExactlyElementsOf(ALL);
      assertClean(lang, html);
      assertThat(html.replaceAll("\\s+", " "))
          .contains("Example Landlord BV")
          .contains(lang.depositTerm())
          .contains(lang.articleRef());
    }
    assertThat(render(lang, ALL, true)).contains("EUR 2,500.00").contains("31 October 2028");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("languages")
  @DisplayName("only required clauses: optional bodies absent, references to them omitted")
  void requiredOnly(Lang lang) {
    String html = render(lang, REQUIRED, true);
    assertThat(renderedClauses(html)).containsExactlyElementsOf(REQUIRED);
    assertThat(html)
        .doesNotContain("data-clause=\"deposit\"")
        .doesNotContain("data-ref=\"deposit\"")
        .doesNotContain("data-ref=\"rent-adjustment\"");
    assertClean(lang, html);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("languages")
  @DisplayName("payment: a due day other than the 1st is not called payment in advance")
  void paymentDueDay(Lang lang) {
    String first = clauseBody(render(lang, ALL, true, Map.of("paymentDueDay", 1)), "payment");
    assertThat(first).contains(lang.inAdvance()).contains(lang.dueDay1());
    String fifth = clauseBody(render(lang, ALL, true, Map.of("paymentDueDay", 5)), "payment");
    assertThat(fifth).contains(lang.dueDay5()).doesNotContain(lang.inAdvance());
  }

  private static final Pattern DUTCH_MARKERS =
      Pattern.compile(
          "\\b(?:de verhuurder|tenzij|overeenkomst|zie artikel|deurwaardersexploot)\\b",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern PARENTHETICAL = Pattern.compile("\\([^()]*\\)");

  /**
   * Dutch marker words left in the text outside parentheses (Dutch terms are only allowed as
   * parenthetical glosses after the translated term).
   */
  static List<String> dutchOutsideParentheses(String html) {
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
    Matcher m = DUTCH_MARKERS.matcher(text.replaceAll("\\s+", " "));
    while (m.find()) {
      found.add(m.group());
    }
    return found;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("languages")
  @DisplayName("no Dutch marker words outside parenthetical glosses")
  void noDutchOutsideParentheses(Lang lang) {
    assertThat(dutchOutsideParentheses(render(lang, ALL, true))).isEmpty();
    assertThat(dutchOutsideParentheses(render(lang, REQUIRED, false))).isEmpty();
  }

  @org.junit.jupiter.api.Test
  @DisplayName(
      "Dutch marker check: bites on the Dutch text and on an injected term, spares glosses")
  void dutchMarkerCheckBites() throws java.io.IOException {
    String nl =
        new String(
            new org.springframework.core.io.ClassPathResource(
                    "templates/documents/lease-agreement/NL/residential/nl.html")
                .getInputStream()
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
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

  private static String clauseBody(String html, String key) {
    int start = html.indexOf("data-clause=\"" + key + "\"");
    assertThat(start).as("clause %s rendered", key).isPositive();
    int end = html.indexOf("class=\"clause-title\"", start);
    return html.substring(start, end < 0 ? html.length() : end).replaceAll("\\s+", " ");
  }
}
