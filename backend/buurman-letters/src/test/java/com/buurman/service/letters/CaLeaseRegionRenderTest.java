package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/**
 * The CA lease documents branch on the province: Ontario (ON), Quebec (QC) and British Columbia
 * (BC) get their own paragraphs, every other province or territory, an unknown region and a
 * contract of another country with the same code ("ON" in another country, "NL" = Newfoundland and
 * Labrador only in Canada) get the general text. Both languages carry the same branch structure
 * (fidelity gate), so each case runs in English and French.
 */
@DisplayName("CA lease: province branches")
class CaLeaseRegionRenderTest {

  private static final String DEPOSIT = "CAD 1,250.00";
  private static final String END_DATE = "30 June 2027";

  /** Distinctive text per language and branch, as rendered. */
  private static final Map<String, Map<String, String>> PREMISES =
      Map.of(
          "en",
          Map.of(
              "ON", "THIS DOCUMENT IS NOT THE STANDARD FORM OF LEASE",
              "QC", "THIS DOCUMENT IS NOT THE MANDATORY LEASE FORM",
              "BC", "The dwelling is in British Columbia.",
              "GENERAL", "Residential tenancies in Canada are governed by the law of the province"),
          "fr",
          Map.of(
              "ON", "PAS LE BAIL STANDARD",
              "QC", "PAS LE FORMULAIRE DE BAIL OBLIGATOIRE",
              "BC", "Le logement est situé en Colombie-Britannique.",
              "GENERAL", "la location résidentielle est régie par le droit de la province"));

  private static final Map<String, Map<String, String>> TERMINATION =
      Map.of(
          "en",
          Map.of(
              "ON", "not earlier than the 7th day after the notice is given",
              "QC", "(article 1960)",
              "BC", "(section 46, subsections 1 and 4)",
              "GENERAL", "Each party may end the tenancy only on the grounds"),
          "fr",
          Map.of(
              "ON", "au plus tôt le 7e jour qui suit",
              "QC", "(article 1960)",
              "BC", "(article 46, paragraphes 1 et 4)",
              "GENERAL", "partie ne peut mettre fin à la location que pour les motifs"));

  private static final Map<String, Map<String, String>> COMMERCIAL_TERMINATION =
      Map.of(
          "en",
          Map.of(
              "ON", "(section 18, subsection 1, of the Commercial Tenancies Act)",
              "QC", "(article 1882)",
              "BC", "distress under the Rent Distress Act",
              "GENERAL", "depend on the law of the province or territory"),
          "fr",
          Map.of(
              "ON", "(article 18, paragraphe 1, de la Loi sur la location commerciale)",
              "QC", "(article 1882)",
              "BC", "en vertu du Rent Distress Act",
              "GENERAL", "relèvent du droit de la province ou du territoire"));

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
      String kind,
      String language,
      String clauseKey,
      String countryCode,
      String regionCode,
      boolean fixedTerm,
      String depositAmount) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", clauseKey);
    clause.put("title", "TITLE-" + clauseKey);
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/CA/" + kind + "/" + language);
    vars.put("authoritative", language.equals("en"));
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord Inc.");
    vars.put("tenantNames", "J. Tremblay");
    vars.put("propertyAddress", "100 Main Street, Toronto ON");
    vars.put("startDate", "1 July 2026");
    vars.put("endDate", fixedTerm ? END_DATE : null);
    vars.put("fixedTerm", fixedTerm);
    vars.put("rentAmount", "CAD 2,000.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("paymentDueDay", 1);
    vars.put("depositAmount", depositAmount);
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", Map.of(clauseKey, 1));
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private static void assertOnlyBranch(
      String html, Map<String, String> texts, String expectedBranch) {
    assertThat(html).doesNotContain("${").contains(texts.get(expectedBranch));
    texts.forEach(
        (branch, text) -> {
          if (!branch.equals(expectedBranch)) {
            assertThat(html).as("branch %s absent", branch).doesNotContain(text);
          }
        });
  }

  @ParameterizedTest(name = "{0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "en, CA, ON, ON",
        "fr, CA, ON, ON",
        "en, CA, QC, QC",
        "fr, CA, QC, QC",
        "en, CA, BC, BC",
        "fr, CA, BC, BC",
        "en, CA, AB, GENERAL",
        "fr, CA, NL, GENERAL",
        "en, CA, NULL, GENERAL",
        "fr, CA, NULL, GENERAL",
        "en, US, ON, GENERAL",
        "fr, FR, QC, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("residential premises: official-form warnings only in Ontario and Quebec")
  void residentialPremises(String language, String country, String region, String branch) {
    String html = render("residential", language, "premises", country, region, false, DEPOSIT);
    assertOnlyBranch(html, PREMISES.get(language), branch);
  }

  @ParameterizedTest(name = "{0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "en, CA, ON, ON",
        "fr, CA, ON, ON",
        "en, CA, QC, QC",
        "fr, CA, QC, QC",
        "en, CA, BC, BC",
        "fr, CA, BC, BC",
        "en, CA, SK, GENERAL",
        "fr, CA, NULL, GENERAL",
        "en, GB, BC, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("residential termination: province rules, general text elsewhere")
  void residentialTermination(String language, String country, String region, String branch) {
    String html = render("residential", language, "termination", country, region, false, null);
    assertOnlyBranch(html, TERMINATION.get(language), branch);
  }

  @ParameterizedTest(name = "{0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "en, CA, ON, ON",
        "fr, CA, ON, ON",
        "en, CA, QC, QC",
        "fr, CA, QC, QC",
        "en, CA, BC, BC",
        "fr, CA, BC, BC",
        "en, CA, MB, GENERAL",
        "fr, CA, NULL, GENERAL",
        "en, DE, QC, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("commercial termination: re-entry, resiliation and distress by province")
  void commercialTermination(String language, String country, String region, String branch) {
    String html = render("commercial", language, "termination", country, region, true, null);
    assertOnlyBranch(html, COMMERCIAL_TERMINATION.get(language), branch);
  }

  @ParameterizedTest(name = "{0} {1} deposit printed={2}")
  @CsvSource(
      value = {
        "en, ON, true",
        "fr, ON, true",
        "en, QC, false",
        "fr, QC, false",
        "en, BC, true",
        "fr, BC, true",
        "en, NS, true",
        "fr, NULL, true"
      },
      nullValues = "NULL")
  @DisplayName("residential deposit: no amount in Quebec (art. 1904 C.C.Q.), printed elsewhere")
  void residentialDeposit(String language, String region, boolean printed) {
    String html = render("residential", language, "deposit", "CA", region, false, DEPOSIT);
    if (printed) {
      assertThat(html).contains(DEPOSIT);
    } else {
      assertThat(html).doesNotContain(DEPOSIT).contains("1904");
    }
    assertThat(render("residential", language, "deposit", "CA", region, false, null))
        .doesNotContain(DEPOSIT)
        .doesNotContain("${");
  }

  @ParameterizedTest(name = "{0} {1} fixedTerm={2}")
  @CsvSource(
      value = {
        "en, BC, true, true",
        "fr, BC, true, true",
        "en, BC, false, false",
        "fr, BC, false, false",
        "en, ON, true, false",
        "fr, QC, true, false",
        "en, NULL, true, false"
      },
      nullValues = "NULL")
  @DisplayName(
      "residential term: end date printed, BC vacate-clause tick box only for a fixed term")
  void residentialTerm(String language, String region, boolean fixedTerm, boolean vacateBox) {
    String html = render("residential", language, "term", "CA", region, fixedTerm, null);
    if (fixedTerm) {
      assertThat(html).contains(END_DATE);
    } else {
      assertThat(html).doesNotContain(END_DATE);
    }
    String box =
        language.equals("en") ? "Requirement to vacate" : "Obligation de quitter les lieux";
    if (vacateBox) {
      assertThat(html).contains(box);
    } else {
      assertThat(html).doesNotContain(box);
    }
  }

  @Test
  @DisplayName("the Canadian citation pattern pairs section/article and subsection digits")
  void citationPattern() {
    Optional<java.util.regex.Pattern> ca = Optional.of(LeaseDocumentRegistry.CANADIAN_CITATION);
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>See section 12.1, subsection 5, of the Act and section 47.0.1.</p>", ca))
        .containsExactly("0: 12.1 5", "0: 47.0.1");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>Voir l'article 12.1, paragraphe 5, de la Loi et l'article 47.0.1.</p>", ca))
        .containsExactly("0: 12.1 5", "0: 47.0.1");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>Under article 1978.2 C.C.Q. and articles 1957 to 1970.</p>", ca))
        .containsExactly("0: 1957 1970", "0: 1978.2");
    // a letter suffix glued to the number stays in the token and is not dropped
    assertThat(LeaseDocumentFidelityTest.citations("<p>Section 13a, subsection 2.</p>", ca))
        .containsExactly("0: 13a 2");
    // a swapped subsection digit is a different token
    assertThat(
            LeaseDocumentFidelityTest.citations("<p>section 106, subsection 2, of the Act</p>", ca))
        .isNotEqualTo(
            LeaseDocumentFidelityTest.citations("<p>article 106, paragraphe 6, de la Loi</p>", ca));
  }
}
