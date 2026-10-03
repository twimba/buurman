package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/** Real Thymeleaf engine and bundles over the English courtesy translation (no PDF). */
@DisplayName("EN residential lease document render")
class EnResidentialLeaseRenderTest {

  private static final String SOURCE = "lease-agreement/NL/residential/en";

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
  private static final String COURTESY =
      "This is a courtesy translation. Only the version in the national language of the country is"
          + " legally authoritative.";

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

  private String render(List<String> orderedKeys, boolean withOptionalValues) {
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
    vars.put("clauseSource", SOURCE);
    vars.put("authoritative", false);
    vars.put("fallbackUsed", false);
    vars.put("languageUsed", "en");
    vars.put("requestedLang", "en");
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
    Context ctx = new Context(Locale.ENGLISH);
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

  private static void assertCleanEnglish(String html) {
    assertThat(html)
        .doesNotContain("${")
        .doesNotContain("#{")
        .doesNotContain("[[")
        .doesNotContain("null")
        .doesNotContain("??")
        .contains("Ref.: CON01TEST")
        .contains(COURTESY);
    assertThat(html)
        .as("raw bundle key rendered")
        .doesNotContainPattern(NlResidentialLeaseRenderTest.RAW_KEY);
    assertThat(html).doesNotContain("artikel").doesNotContain("de huurder");
  }

  @Test
  @DisplayName("all clauses included: every clause renders in English with the courtesy notice")
  void allIncluded() {
    for (boolean withOptionalValues : List.of(true, false)) {
      String html = render(ALL, withOptionalValues);
      assertThat(renderedClauses(html)).containsExactlyElementsOf(ALL);
      assertCleanEnglish(html);
      assertThat(html)
          .contains("Example Landlord BV")
          .contains("security deposit (waarborgsom)")
          .contains("(see article <span>");
    }
    assertThat(render(ALL, true)).contains("EUR 2,500.00").contains("31 October 2028");
  }

  @Test
  @DisplayName("only required clauses: optional bodies absent, references to them omitted")
  void requiredOnly() {
    String html = render(REQUIRED, true);
    assertThat(renderedClauses(html)).containsExactlyElementsOf(REQUIRED);
    assertThat(html)
        .doesNotContain("data-clause=\"deposit\"")
        .doesNotContain("data-ref=\"deposit\"")
        .doesNotContain("data-ref=\"rent-adjustment\"");
    assertCleanEnglish(html);
  }
}
