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
import com.buurman.domain.LeaseKind;

/**
 * Belgian residential lease law is regional: the BE documents branch on the catalog region codes
 * VLG (Flanders), WAL (Wallonia) and BRU (Brussels-Capital) of Belgium, with a general text for an
 * unknown region or a region code of another country. The commercial lease branches only where the
 * regional versions of the Handelshuurwet differ (the Walloon form of a mutual termination
 * agreement). Every language carries the same branch structure (fidelity gate).
 */
@DisplayName("BE leases: region branches")
class BeLeaseRegionRenderTest {

  /** Residential deposit clause: one distinctive phrase per branch and language. */
  private static final Map<String, Map<String, String>> DEPOSIT =
      Map.of(
          "VLG",
          Map.of(
              "nl", "artikel 37, § 1, van het Vlaams Woninghuurdecreet",
              "fr", "article 37, § 1er, du décret flamand sur la location d'habitations",
              "en", "article 37, § 1, of the Flemish Residential Lease Decree"),
          "WAL",
          Map.of(
              "nl", "artikel 62, § 1, van het Waals decreet betreffende de woninghuurovereenkomst",
              "fr", "article 62, § 1er, du décret wallon relatif au bail d'habitation",
              "en", "article 62, § 1, of the Walloon Residential Lease Decree"),
          "BRU",
          Map.of(
              "nl", "artikel 248 van de Brusselse Huisvestingscode",
              "fr", "article 248 du Code bruxellois du Logement",
              "en", "article 248 of the Brussels Housing Code"),
          "GENERAL",
          Map.of(
              "nl",
                  "Het maximumbedrag van de huurwaarborg en de toegelaten vormen verschillen per"
                      + " gewest",
              "fr", "Le montant maximal de la garantie locative et les formes autorisées diffèrent",
              "en", "The maximum amount of the rental guarantee and the permitted forms differ"));

  /** Commercial term clause: the form of a mutual termination agreement. */
  private static final Map<String, Map<String, String>> MUTUAL_TERMINATION =
      Map.of(
          "WAL",
          Map.of(
              "nl", "zoals van toepassing in het Waalse Gewest",
              "fr", "tel qu'applicable en Région wallonne",
              "en", "as applicable in the Walloon Region"),
          "FEDERAL",
          Map.of(
              "nl", "bij een authentieke akte of bij een verklaring voor de rechter afgelegd",
              "fr", "constaté par acte authentique ou par une déclaration faite devant le juge",
              "en", "recorded in a notarial deed or in a declaration made before the court"),
          "GENERAL",
          Map.of(
              "nl", "in de vorm die het gewest voorschrijft",
              "fr", "dans la forme prescrite par la Région",
              "en", "in the form the region prescribes"));

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

  private String render(
      String kind, String clauseKey, String language, String countryCode, String regionCode) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", clauseKey);
    clause.put("title", "TITLE-" + clauseKey);
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/BE/" + kind + "/" + language);
    vars.put("authoritative", language.equals("nl"));
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("startDate", "1 November 2026");
    vars.put("endDate", null);
    vars.put("fixedTerm", false);
    vars.put("depositAmount", "EUR 2,400.00");
    vars.put("rentAmount", "EUR 1,200.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", Map.of(clauseKey, 1));
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private static void assertOnlyBranch(
      String html, Map<String, Map<String, String>> phrases, String expected, String language) {
    assertThat(html).doesNotContain("${").doesNotContain("??");
    phrases.forEach(
        (branch, byLanguage) -> {
          if (branch.equals(expected)) {
            assertThat(html).as("branch %s", branch).contains(byLanguage.get(language));
          } else {
            assertThat(html).as("branch %s", branch).doesNotContain(byLanguage.get(language));
          }
        });
  }

  @ParameterizedTest(name = "residential {0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "nl, BE, VLG, VLG",
        "fr, BE, VLG, VLG",
        "en, BE, VLG, VLG",
        "nl, BE, WAL, WAL",
        "fr, BE, WAL, WAL",
        "en, BE, WAL, WAL",
        "nl, BE, BRU, BRU",
        "fr, BE, BRU, BRU",
        "en, BE, BRU, BRU",
        "nl, BE, NULL, GENERAL",
        "fr, BE, NULL, GENERAL",
        "en, BE, NULL, GENERAL",
        "nl, BE, ANT, GENERAL",
        "en, FR, VLG, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("residential deposit: one regional branch per region, general text otherwise")
  void residentialDepositBranch(String language, String country, String region, String branch) {
    String html = render("residential", "deposit", language, country, region);
    assertThat(html).contains("data-clause=\"deposit\"").contains("EUR 2,400.00");
    assertOnlyBranch(html, DEPOSIT, branch, language);
  }

  @ParameterizedTest(name = "residential {0}: every clause renders for {1}")
  @CsvSource(
      value = {
        "nl, VLG", "nl, WAL", "nl, BRU", "fr, VLG", "fr, WAL", "fr, BRU", "en, VLG", "en, WAL",
        "en, BRU"
      })
  @DisplayName("residential: every clause renders cleanly in each region")
  void residentialEveryClauseRendersPerRegion(String language, String region) {
    for (String key :
        LeaseDocumentRegistry.find("BE", LeaseKind.RESIDENTIAL).orElseThrow().clauseKeys()) {
      String html = render("residential", key, language, "BE", region);
      assertThat(html)
          .as("%s in %s", key, region)
          .contains("data-clause=\"" + key + "\"")
          .doesNotContain("${")
          .doesNotContain("??");
    }
  }

  @ParameterizedTest(name = "commercial {0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "nl, BE, WAL, WAL",
        "fr, BE, WAL, WAL",
        "en, BE, WAL, WAL",
        "nl, BE, VLG, FEDERAL",
        "fr, BE, BRU, FEDERAL",
        "en, BE, VLG, FEDERAL",
        "nl, BE, NULL, GENERAL",
        "fr, BE, NULL, GENERAL",
        "en, CA, WAL, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("commercial term: Walloon form of a mutual termination agreement")
  void commercialMutualTerminationBranch(
      String language, String country, String region, String branch) {
    String html = render("commercial", "term", language, country, region);
    assertThat(html).contains("data-clause=\"term\"");
    assertOnlyBranch(html, MUTUAL_TERMINATION, branch, language);
  }
}
