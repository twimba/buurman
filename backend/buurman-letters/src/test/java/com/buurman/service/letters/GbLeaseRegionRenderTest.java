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
 * The GB lease documents (English only) branch per nation on the contract region: ENG (England),
 * WLS (Wales), SCT (Scotland), NIR (Northern Ireland), with a general text for an unknown region or
 * another country with the same code. Residential clauses state each nation's tenancy regime;
 * commercial clauses group England and Wales (Landlord and Tenant Act 1954) against Scotland and
 * Northern Ireland.
 */
@DisplayName("GB lease: nation branches")
class GbLeaseRegionRenderTest {

  private static final String GENERAL = "GENERAL";

  /** Residential termination clause: a distinctive phrase per nation. */
  private static final Map<String, String> RESIDENTIAL_TERMINATION =
      Map.of(
          "ENG",
          "notices under section 21 of that Act can no longer be given",
          "WLS",
          "by a notice under section 173 of the Renting Homes (Wales) Act 2016",
          "SCT",
          "notice to leave stating 1 or more of the eviction grounds",
          "NIR",
          "notice where it has existed for more than 12 months but no more than 10 years",
          GENERAL,
          "The tenancy may be ended only as the law of the nation");

  /** Residential deposit clause: a distinctive phrase per nation. */
  private static final Map<String, String> RESIDENTIAL_DEPOSIT =
      Map.of(
          "ENG",
          "where the annual rent is less than £50,000",
          "WLS",
          "section 45 of the Renting Homes (Wales) Act 2016",
          "SCT",
          "The deposit may not exceed 2 months",
          "NIR",
          "The deposit may not exceed 1 month",
          GENERAL,
          "England, Scotland and Northern Ireland also limit the amount");

  /** Commercial security of tenure clause: England and Wales share the 1954 Act. */
  private static final Map<String, String> COMMERCIAL_TENURE =
      Map.of(
          "ENG",
          "Part II of the Landlord and Tenant Act 1954 may give the tenant",
          "WLS",
          "Part II of the Landlord and Tenant Act 1954 may give the tenant",
          "SCT",
          "the lease continues by tacit relocation, for 1 year",
          "NIR",
          "the Business Tenancies (Northern Ireland) Order 1996 may give the tenant",
          GENERAL,
          "depends on the jurisdiction in which the premises are located");

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
      String clauseKey,
      String countryCode,
      String regionCode,
      boolean fixedTerm,
      Map<String, Integer> refs) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", clauseKey);
    clause.put("title", "TITLE-" + clauseKey);
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/GB/" + kind + "/en");
    vars.put("authoritative", true);
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", "en");
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord Ltd");
    vars.put("tenantNames", "A. Smith");
    vars.put("propertyAddress", "1 High Street, London");
    vars.put("startDate", "1 November 2026");
    vars.put("endDate", "31 October 2027");
    vars.put("fixedTerm", fixedTerm);
    vars.put("rentAmount", "£1,250.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("paymentDueDay", 1);
    vars.put("depositAmount", "£1,400.00");
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", refs);
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private static String expectedKey(String country, String region) {
    return "GB".equals(country)
            && region != null
            && List.of("ENG", "WLS", "SCT", "NIR").contains(region)
        ? region
        : GENERAL;
  }

  private static void assertOnlyBranch(String html, Map<String, String> phrases, String expected) {
    assertThat(html).doesNotContain("${").contains(phrases.get(expected));
    phrases.forEach(
        (key, phrase) -> {
          if (!phrase.equals(phrases.get(expected))) {
            assertThat(html).as("branch %s must not render", key).doesNotContain(phrase);
          }
        });
  }

  @ParameterizedTest(name = "{0}/{1}")
  @CsvSource(
      value = {
        "GB, ENG",
        "GB, WLS",
        "GB, SCT",
        "GB, NIR",
        "GB, NULL",
        "GB, LND",
        "IE, NIR",
        "US, ENG"
      },
      nullValues = "NULL")
  @DisplayName("residential termination and deposit print exactly the nation's branch")
  void residentialBranches(String country, String region) {
    String expected = expectedKey(country, region);
    assertOnlyBranch(
        render("residential", "termination", country, region, false, Map.of()),
        RESIDENTIAL_TERMINATION,
        expected);
    assertOnlyBranch(
        render("residential", "deposit", country, region, false, Map.of()),
        RESIDENTIAL_DEPOSIT,
        expected);
  }

  @ParameterizedTest(name = "{0}/{1}")
  @CsvSource(
      value = {"GB, ENG", "GB, WLS", "GB, SCT", "GB, NIR", "GB, NULL", "FR, SCT"},
      nullValues = "NULL")
  @DisplayName("commercial security of tenure prints exactly the jurisdiction's branch")
  void commercialBranches(String country, String region) {
    String expected = expectedKey(country, region);
    String html = render("commercial", "security-of-tenure", country, region, true, Map.of());
    assertThat(html).contains(COMMERCIAL_TENURE.get(expected));
    COMMERCIAL_TENURE.forEach(
        (key, phrase) -> {
          if (!phrase.equals(COMMERCIAL_TENURE.get(expected))) {
            assertThat(html).as("branch %s must not render", key).doesNotContain(phrase);
          }
        });
  }

  private static final String GENERAL_PERIODIC =
      "The tenancy is periodic and continues from rent period to rent period until it is ended in"
          + " accordance with the law of the nation";
  private static final String GENERAL_FIXED =
      "insofar as the law of the nation in which the dwelling is located permits a fixed term";

  @ParameterizedTest(name = "{0} fixedTerm={1}")
  @CsvSource(
      delimiter = '|',
      nullValues = "NULL",
      value = {
        "ENG|true|has no legal effect in England|NONE|true",
        "ENG|false|The tenancy is a periodic tenancy and continues from rent period|"
            + "has no legal effect|false",
        "SCT|true|does not end the tenancy|NONE|true",
        "SCT|false|A private residential tenancy has no end date|does not end the tenancy|false",
        "WLS|true|This contract is a fixed term standard contract|"
            + "This contract is a periodic standard contract|true",
        "WLS|false|This contract is a periodic standard contract|"
            + "This contract is a fixed term standard contract|false",
        "NIR|true|The tenancy is granted for a fixed term|ended by notice to quit or"
            + " otherwise|true",
        "NIR|false|ended by notice to quit or otherwise|The tenancy is granted for a fixed"
            + " term|false",
        "NULL|true|" + GENERAL_FIXED + "|" + GENERAL_PERIODIC + "|true",
        "NULL|false|" + GENERAL_PERIODIC + "|" + GENERAL_FIXED + "|false"
      })
  @DisplayName("residential term: every nation and the general text, fixed term and periodic")
  void residentialTerm(
      String region, boolean fixedTerm, String present, String absent, boolean endDatePrinted) {
    String html = render("residential", "term", "GB", region, fixedTerm, Map.of());
    assertThat(html).contains(present).doesNotContain("${");
    if (!"NONE".equals(absent)) {
      assertThat(html).doesNotContain(absent);
    }
    if (endDatePrinted) {
      assertThat(html).contains("31 October 2027");
    } else {
      assertThat(html).doesNotContain("31 October 2027");
    }
  }

  @ParameterizedTest(name = "{0} fixedTerm={1}")
  @CsvSource(
      value = {"ENG, true", "ENG, false", "SCT, true", "SCT, false", "NULL, true", "NULL, false"},
      nullValues = "NULL")
  @DisplayName("commercial term: fixed term with end date, or periodic tenancy")
  void commercialTerm(String region, boolean fixedTerm) {
    String html = render("commercial", "term", "GB", region, fixedTerm, Map.of());
    if (fixedTerm) {
      assertThat(html)
          .contains("The lease is granted for a fixed term")
          .contains("31 October 2027")
          .doesNotContain("granted as a periodic tenancy");
    } else {
      assertThat(html)
          .contains("The lease is granted as a periodic tenancy")
          .doesNotContain("granted for a fixed term")
          .doesNotContain("31 October 2027");
    }
    assertThat(html.contains("registered at HM Land Registry"))
        .as("Land Registry sentence only in England and Wales")
        .isEqualTo("ENG".equals(region));
  }

  @Test
  @DisplayName("cross-references sit outside the nation branches and render for every nation")
  void crossReferencesRenderForEveryNation() {
    for (String region : List.of("ENG", "WLS", "SCT", "NIR", "XX")) {
      String html =
          render("residential", "deposit", "GB", region, false, Map.of("handover-inspection", 7));
      assertThat(html)
          .as(region)
          .contains("data-ref=\"handover-inspection\"")
          .contains("(see article <span>7</span>)");
    }
  }

  @Test
  @DisplayName("the UK citation pattern pairs a section, regulation or ground with its number")
  void ukCitationPattern() {
    Optional<java.util.regex.Pattern> uk = Optional.of(LeaseDocumentRegistry.UK_CITATION);
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>Under section 13 of the Act, s.21, regulation 36 and Ground 1A.</p>", uk))
        .hasSize(4)
        .anyMatch(c -> c.endsWith(": 13"))
        .anyMatch(c -> c.endsWith(": 21"))
        .anyMatch(c -> c.endsWith(": 36"))
        .anyMatch(c -> c.endsWith(": 1A"));
    assertThat(
            LeaseDocumentFidelityTest.citations("<p>section 13 subsection 4A of that Act</p>", uk))
        .singleElement()
        .satisfies(c -> assertThat(c).endsWith(": 13 4"));
  }
}
