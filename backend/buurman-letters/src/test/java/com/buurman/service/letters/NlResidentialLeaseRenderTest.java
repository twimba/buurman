package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/** Real Thymeleaf engine and bundles over the shipped NL residential document (no PDF). */
@DisplayName("NL residential lease document render")
class NlResidentialLeaseRenderTest {

  private static final String SOURCE = "lease-agreement/NL/residential/nl";

  /** Template order of V092. */
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
  private static final Pattern REF =
      Pattern.compile("data-ref=\"([a-z0-9-]+)\"[^>]*>[^<]*?\\(zie artikel <span>(\\d+)</span>\\)");

  /** A bundle key printed verbatim (missing key with useCodeAsDefaultMessage). */
  static final Pattern RAW_KEY = Pattern.compile(">\\s*lease\\.[a-zA-Z.-]+");

  private TemplateEngine engine;

  @BeforeEach
  void setUp() {
    engine =
        DocumentTemplateSupport.templateEngine(
            DocumentTemplateSupport.messageSource(
                false,
                "classpath:messages/document-letter-chrome",
                "classpath:messages/document-lease-agreement"),
            false);
  }

  private static Map<String, Object> baseVariables(boolean withOptionalValues) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", SOURCE);
    vars.put("authoritative", true);
    vars.put("fallbackUsed", false);
    vars.put("languageUsed", "nl");
    vars.put("requestedLang", "nl");
    vars.put("generatedDate", "3 oktober 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("propertyAddress", "Keizersgracht 1, 1015 AA Amsterdam");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Verhuur Voorbeeld BV");
    vars.put("tenantNames", "J. Jansen, P. de Vries");
    vars.put("startDate", "1 november 2026");
    vars.put("contractTypeLabel", "Onbepaalde tijd");
    vars.put("rentAmount", "€ 1.250,00");
    vars.put("paymentFrequency", "per maand");
    vars.put("landlordNoticeDays", 90);
    vars.put("tenantNoticeDays", 30);
    vars.put("countryMetadata", null);
    vars.put("endDate", withOptionalValues ? "31 oktober 2028" : null);
    vars.put("depositAmount", withOptionalValues ? "€ 2.500,00" : null);
    vars.put("paymentDueDay", withOptionalValues ? 1 : null);
    vars.put("fixedTerm", withOptionalValues);
    return vars;
  }

  /** Numbers the given keys 1..n in the given order, as LeaseClauseResolver would. */
  private String render(List<String> orderedKeys, boolean withOptionalValues) {
    return render(orderedKeys, withOptionalValues, Map.of());
  }

  private String render(
      List<String> orderedKeys, boolean withOptionalValues, Map<String, Object> overrides) {
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
    Map<String, Object> vars = baseVariables(withOptionalValues);
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    overrides.forEach(vars::put);
    Context ctx = new Context(Locale.forLanguageTag("nl"));
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

  private static Map<String, List<Integer>> renderedRefs(String html) {
    Map<String, List<Integer>> refs = new HashMap<>();
    Matcher m = REF.matcher(html);
    while (m.find()) {
      refs.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(Integer.parseInt(m.group(2)));
    }
    return refs;
  }

  private static void assertNoLeaks(String html) {
    assertThat(html)
        .doesNotContain("${")
        .doesNotContain("#{")
        .doesNotContain("[[")
        .doesNotContain("null")
        .doesNotContain("??");
    assertThat(html).as("raw bundle key rendered").doesNotContainPattern(RAW_KEY);
    assertThat(html).contains("Kenmerk: CON01TEST");
  }

  /** The "clause-<key>" body of one rendered clause, up to the next clause title. */
  private static String clauseBody(String html, String key) {
    int start = html.indexOf("data-clause=\"" + key + "\"");
    assertThat(start).as("clause %s rendered", key).isPositive();
    int end = html.indexOf("class=\"clause-title\"", start);
    return html.substring(start, end < 0 ? html.length() : end).replaceAll("\\s+", " ");
  }

  private static Map<String, Object> vars(Object... keyValues) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      m.put((String) keyValues[i], keyValues[i + 1]);
    }
    return m;
  }

  @Test
  @DisplayName("an indefinite contract that carries an end date is still rendered as indefinite")
  void indefiniteWithEndDate() {
    String html = render(ALL, true, vars("fixedTerm", false, "endDate", "31 oktober 2028"));
    String term = clauseBody(html, "term");
    assertThat(term).contains("voor onbepaalde tijd").doesNotContain("31 oktober 2028");
    assertThat(clauseBody(html, "termination")).doesNotContain("bepaalde tijd is aangegaan");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("a fixed-term contract states that the lease does not end by mere expiry")
  void fixedTermWithEndDate() {
    String html = render(ALL, true, vars("fixedTerm", true, "endDate", "31 oktober 2028"));
    String term = clauseBody(html, "term");
    assertThat(term)
        .contains("31 oktober 2028")
        .contains("eindigt niet door het enkele verloop van de huurtijd")
        .contains("niet eerder dan tegen het einde van de bepaalde tijd");
    String termination = clauseBody(html, "termination");
    assertThat(termination).contains("in afwijking hiervan").doesNotContain("daarnaast");
    assertThat(renderedRefs(termination).get("term")).containsOnly(ALL.indexOf("term") + 1);
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("a fixed-term contract without an end date renders without residue")
  void fixedTermWithoutEndDate() {
    String html = render(ALL, true, vars("fixedTerm", true, "endDate", null));
    assertThat(clauseBody(html, "term")).contains("voor bepaalde tijd");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("fixed-term termination wording keeps working when the term clause is renumbered")
  void fixedTermTerminationRef() {
    List<String> reordered = new ArrayList<>(ALL);
    reordered.remove("term");
    reordered.add(3, "term");
    String html = render(reordered, true, vars("fixedTerm", true));
    assertThat(renderedRefs(clauseBody(html, "termination")).get("term")).containsOnly(4);
  }

  @Test
  @DisplayName("payment: due day other than the 1st is not called advance payment")
  void paymentDueDay() {
    String first = clauseBody(render(ALL, true, vars("paymentDueDay", 1)), "payment");
    assertThat(first)
        .contains("bij vooruitbetaling")
        .contains("<strong>1</strong>e dag van de maand waarin de betalingstermijn aanvangt");
    String fifth = clauseBody(render(ALL, true, vars("paymentDueDay", 5)), "payment");
    assertThat(fifth)
        .doesNotContain("vooruitbetaling")
        .contains("<strong>5</strong>e dag van de maand waarin de betalingstermijn aanvangt")
        .doesNotContain("van iedere betalingstermijn");
  }

  @Test
  @DisplayName("no tenant names: no empty name element")
  void emptyTenantNames() {
    String html = render(ALL, true, vars("tenantNames", ""));
    assertThat(clauseBody(html, "parties")).doesNotContain("<strong></strong>");
  }

  @Test
  @DisplayName("all clauses included: every clause body renders, in order, with no residue")
  void allIncluded() {
    for (boolean withOptionalValues : List.of(true, false)) {
      String html = render(ALL, withOptionalValues);
      assertThat(renderedClauses(html)).containsExactlyElementsOf(ALL);
      assertNoLeaks(html);
      assertThat(html).contains("Verhuur Voorbeeld BV").contains("J. Jansen, P. de Vries");
      assertThat(renderedRefs(html)).isNotEmpty();
    }
  }

  @Test
  @DisplayName("only required clauses: optional bodies absent, references to them omitted")
  void requiredOnly() {
    String html = render(REQUIRED, true);
    assertThat(renderedClauses(html)).containsExactlyElementsOf(REQUIRED);
    assertThat(html).doesNotContain("data-clause=\"deposit\"");
    Set<String> referenced = renderedRefs(html).keySet();
    assertThat(REQUIRED).containsAll(referenced);
    assertThat(html)
        .doesNotContain("data-ref=\"deposit\"")
        .doesNotContain("data-ref=\"rent-adjustment\"");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("every cross-reference shows the referenced clause's current article number")
  void crossReferencesPointToCurrentNumbers() {
    String html = render(ALL, true);
    renderedRefs(html)
        .forEach(
            (key, numbers) ->
                assertThat(numbers).as("refs to %s", key).containsOnly(ALL.indexOf(key) + 1));
  }

  @Test
  @DisplayName("a reordered clause list renumbers articles and cross-references follow")
  void reorderRenumbers() {
    List<String> reordered = new ArrayList<>(ALL);
    reordered.remove("deposit");
    reordered.remove("handover-inspection");
    reordered.add(2, "handover-inspection");
    reordered.add(2, "deposit");
    String html = render(reordered, true);

    assertThat(renderedClauses(html)).containsExactlyElementsOf(reordered);
    assertThat(html).containsPattern("<span>3</span>\\. <span>TITLE-deposit</span>");
    Map<String, List<Integer>> refs = renderedRefs(html);
    assertThat(refs.get("deposit")).isNotNull().containsOnly(3);
    assertThat(refs.get("handover-inspection")).isNotNull().containsOnly(4);
    refs.forEach((key, numbers) -> assertThat(numbers).containsOnly(reordered.indexOf(key) + 1));
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("a reference to a single excluded clause disappears while the rest stays")
  void singleExclusion() {
    List<String> withoutHandover = new ArrayList<>(ALL);
    withoutHandover.remove("handover-inspection");
    String html = render(withoutHandover, true);

    assertThat(html).contains("data-clause=\"deposit\"");
    assertThat(html).doesNotContain("data-ref=\"handover-inspection\"");
    assertThat(renderedRefs(html).get("deposit")).isNotNull();
    assertNoLeaks(html);
  }
}
