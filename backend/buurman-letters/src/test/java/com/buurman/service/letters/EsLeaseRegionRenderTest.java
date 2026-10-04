package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/**
 * The ES deposit clause (residential and commercial) branches on the region: a contract in
 * Catalonia (catalog code CAT, ISO 3166-2 suffix CT) prints the INCASÒL deposit paragraph (Llei
 * 13/1996); any other or unknown region, or another country with the same code, prints the general
 * paragraph on regional deposit duties (disposición adicional tercera LAU). Both languages carry
 * the same branch structure (fidelity gate).
 */
@DisplayName("ES lease: Catalan deposit branch")
class EsLeaseRegionRenderTest {

  private static final Map<String, String> CATALONIA =
      Map.of("es", "Institut Català del Sòl (INCASÒL)", "en", "Catalan Land Institute");

  private static final Map<String, String> GENERAL =
      Map.of(
          "es", "Pueden aplicarse, por tanto, normas autonómicas sobre el depósito de la fianza.",
          "en", "Regional rules on lodging the deposit (fianza) may therefore apply.");

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

  private String renderDeposit(
      String kind, String language, String countryCode, String regionCode) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", "deposit");
    clause.put("title", "TITLE-deposit");
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/ES/" + kind + "/" + language);
    vars.put("authoritative", language.equals("es"));
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("depositAmount", "EUR 1,250.00");
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", Map.of("deposit", 1));
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  @ParameterizedTest(name = "{0}/{1} {2}/{3} catalonia={4}")
  @CsvSource(
      value = {
        "residential, es, ES, CAT, true",
        "residential, en, ES, CAT, true",
        "residential, es, ES, CT, true",
        "residential, en, ES, CT, true",
        "residential, es, ES, PV, false",
        "residential, en, ES, PV, false",
        "residential, es, ES, NULL, false",
        "residential, en, ES, NULL, false",
        "residential, es, FR, CAT, false",
        "commercial, es, ES, CAT, true",
        "commercial, en, ES, CAT, true",
        "commercial, es, ES, NC, false",
        "commercial, en, ES, NULL, false",
        "commercial, en, IT, CT, false"
      },
      nullValues = "NULL")
  @DisplayName("INCASÒL paragraph only for Catalonia in Spain, general text otherwise")
  void branchesOnRegion(
      String kind, String language, String country, String region, boolean catalonia) {
    String html = renderDeposit(kind, language, country, region);
    assertThat(html).contains("data-clause=\"deposit\"").doesNotContain("${");
    if (catalonia) {
      assertThat(html).contains(CATALONIA.get(language)).doesNotContain(GENERAL.get(language));
    } else {
      assertThat(html).contains(GENERAL.get(language)).doesNotContain(CATALONIA.get(language));
    }
  }
}
