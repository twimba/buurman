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
 * The FR residential rent clause branches on the region: a contract in one of the catalog's FR-*
 * rent-control territories (encadrement du niveau des loyers, art. 140 loi ELAN) prints the
 * territory paragraph with fill-in lines for the reference rents; any other or unknown region
 * prints the general conditional paragraph. Both languages carry the same branch structure
 * (fidelity gate).
 */
@DisplayName("FR residential lease: rent-control region branch")
class FrResidentialLeaseRegionRenderTest {

  private static final Map<String, String> TERRITORY =
      Map.of(
          "fr", "Si ce dispositif s'applique à la date de conclusion du présent contrat",
          "en", "If that scheme applies on the date this agreement is concluded");

  private static final Map<String, String> GENERAL =
      Map.of(
          "fr", "Des règles locales peuvent ainsi s'appliquer.",
          "en", "Local rules may therefore apply.");

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

  private String renderRent(String language, String countryCode, String regionCode) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", "rent");
    clause.put("title", "TITLE-rent");
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/FR/residential/" + language);
    vars.put("authoritative", language.equals("fr"));
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("rentAmount", "EUR 1,250.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", Map.of("rent", 1));
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  @ParameterizedTest(name = "{0} {1}/{2} territory={3}")
  @CsvSource(
      value = {
        "fr, FR, FR-PARIS, true",
        "en, FR, FR-PARIS, true",
        "fr, FR, FR-LYON, true",
        "en, FR, FR-LYON, true",
        "fr, FR, NULL, false",
        "en, FR, NULL, false",
        "fr, FR, IDF, false",
        "en, FR, IDF, false",
        "fr, CA, FR-PARIS, false",
        "en, CA, FR-PARIS, false"
      },
      nullValues = "NULL")
  @DisplayName("territory paragraph only for FR-* regions of France, general text otherwise")
  void branchesOnRegion(String language, String country, String region, boolean territory) {
    String html = renderRent(language, country, region);
    assertThat(html).contains("data-clause=\"rent\"").doesNotContain("${");
    if (territory) {
      assertThat(html).contains(TERRITORY.get(language)).doesNotContain(GENERAL.get(language));
    } else {
      assertThat(html).contains(GENERAL.get(language)).doesNotContain(TERRITORY.get(language));
    }
  }
}
