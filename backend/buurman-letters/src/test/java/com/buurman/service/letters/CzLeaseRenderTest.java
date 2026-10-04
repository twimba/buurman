package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.LeaseKind;

/**
 * The CZ lease documents are English only: Czech is not a document language, so the English text is
 * a convenience text. The locator never marks it authoritative (courtesy notice at runtime) and the
 * documents themselves say that they are drawn up and signed in English only. The residential
 * document branches on the term (fixed: renewal under section 2285; indefinite: section 2204), the
 * deposit and the payment day; the commercial termination clause follows the term (sections 2308 to
 * 2310 for a fixed term, section 2312 for an indefinite one).
 */
@DisplayName("CZ lease: English convenience text, term and termination branches")
class CzLeaseRenderTest {

  private static final String ENGLISH_ONLY = "drawn up and signed in English only";
  private static final String DEPOSIT = "CZK 45,000.00";
  private static final String END_DATE = "31 October 2027";

  private MessageSource messages;
  private TemplateEngine engine;

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
      String kind,
      List<String> clauseKeys,
      boolean authoritative,
      boolean fixedTerm,
      Map<String, Object> overrides) {
    List<Map<String, Object>> clauses =
        clauseKeys.stream()
            .map(
                key -> {
                  Map<String, Object> clause = new HashMap<>();
                  clause.put("clauseKey", key);
                  clause.put("title", "TITLE-" + key);
                  clause.put("articleNumber", clauseKeys.indexOf(key) + 1);
                  return clause;
                })
            .toList();
    Map<String, Integer> refs = new HashMap<>();
    for (int i = 0; i < clauseKeys.size(); i++) {
      refs.put(clauseKeys.get(i), i + 1);
    }
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/CZ/" + kind + "/en");
    vars.put("authoritative", authoritative);
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", "en");
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord s.r.o.");
    vars.put("tenantNames", "J. Novák");
    vars.put("propertyAddress", "Vodičkova 1, 110 00 Praha 1");
    vars.put("startDate", "1 November 2026");
    vars.put("endDate", END_DATE);
    vars.put("fixedTerm", fixedTerm);
    vars.put("rentAmount", "CZK 15,000.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("paymentDueDay", null);
    vars.put("depositAmount", DEPOSIT);
    vars.put("countryCode", "CZ");
    vars.put("regionCode", null);
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    vars.putAll(overrides);
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private static List<String> keys(LeaseKind kind) {
    return LeaseDocumentRegistry.find("CZ", kind).orElseThrow().clauseKeys();
  }

  @Test
  @DisplayName("the locator serves the English CZ document as non-authoritative for any request")
  void locatorMarksEnglishNonAuthoritative() {
    LeaseDocumentLocator locator = new LeaseDocumentLocator();
    for (LeaseKind kind : List.of(LeaseKind.RESIDENTIAL, LeaseKind.COMMERCIAL)) {
      for (String requested : List.of("en", "de", "nl")) {
        LeaseDocumentLocator.LeaseDocument doc =
            locator.locate("CZ", kind, requested).orElseThrow();
        assertThat(doc.templatePath())
            .isEqualTo("lease-agreement/CZ/" + kind.pathSegment() + "/en");
        assertThat(doc.languageUsed()).isEqualTo("en");
        assertThat(doc.authoritative()).isFalse();
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName(
      "the runtime render shows the no-national-version notice (not the courtesy one) and the"
          + " convenience-text statement")
  void convenienceTextAndNoNationalVersionNotice(String kind) {
    LeaseKind leaseKind = kind.equals("residential") ? LeaseKind.RESIDENTIAL : LeaseKind.COMMERCIAL;
    String html = render(kind, keys(leaseKind), false, true, Map.of("noNationalVersion", true));
    assertThat(html)
        .contains(messages.getMessage("lease.notice.noNationalVersion", null, Locale.ENGLISH))
        .doesNotContain(messages.getMessage("lease.notice.courtesy", null, Locale.ENGLISH))
        .contains(ENGLISH_ONLY)
        .contains("checked by a person qualified in Czech law")
        .contains("which version prevails")
        .doesNotContain("${");
  }

  @Test
  @DisplayName("residential term: fixed term prints the end date and the section 2285 renewal")
  void residentialTerm() {
    String fixed = render("residential", List.of("term"), true, true, Map.of());
    assertThat(fixed)
        .contains("fixed term (nájem na dobu určitou)")
        .contains(END_DATE)
        .contains("section 2285 of the Civil Code")
        .doesNotContain("indefinite term");
    String indefinite = render("residential", List.of("term"), true, false, Map.of());
    assertThat(indefinite)
        .contains("indefinite term (nájem na dobu neurčitou)")
        .doesNotContain(END_DATE)
        .doesNotContain("section 2285");
  }

  @Test
  @DisplayName("residential deposit: an agreed deposit is printed, else a fill-in line")
  void residentialDeposit() {
    String set = render("residential", List.of("deposit"), true, true, Map.of());
    assertThat(set).contains(DEPOSIT).doesNotContain("do not agree a security deposit");
    Map<String, Object> noDeposit = new HashMap<>();
    noDeposit.put("depositAmount", null);
    String unset = render("residential", List.of("deposit"), true, true, noDeposit);
    assertThat(unset)
        .doesNotContain(DEPOSIT)
        .contains("do not agree a security deposit (jistota)")
        .contains("3 times the monthly rent");
  }

  @Test
  @DisplayName("residential payment: the 5th day by default, else the agreed day")
  void residentialPaymentDay() {
    String byDefault = render("residential", List.of("payment"), true, true, Map.of());
    assertThat(byDefault).contains("no later than the 5th day of the month");
    String agreed =
        render("residential", List.of("payment"), true, true, Map.of("paymentDueDay", 10));
    assertThat(agreed)
        .contains("no later than day <strong>10</strong> of the month")
        .doesNotContain("no later than the 5th day of the month");
  }

  @Test
  @DisplayName("commercial termination follows the term: sections 2308-2310 or section 2312")
  void commercialTermination() {
    String fixed = render("commercial", List.of("termination"), true, true, Map.of());
    assertThat(fixed)
        .contains("(section 2308 of the Civil Code)")
        .contains("(section 2309 of the Civil Code)")
        .contains("notice period (výpovědní doba) is 3 months")
        .doesNotContain("section 2312");
    String indefinite = render("commercial", List.of("termination"), true, false, Map.of());
    assertThat(indefinite)
        .contains("notice period of 6 months")
        .contains("(section 2312 of the Civil Code)")
        .doesNotContain("section 2308");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName(
      "the language statement and the prevailing-version line survive a required-only render")
  void languageStatementInRequiredParties(String kind) {
    LeaseKind leaseKind = kind.equals("residential") ? LeaseKind.RESIDENTIAL : LeaseKind.COMMERCIAL;
    List<String> required =
        LeaseDocumentRegistry.find("CZ", leaseKind).orElseThrow().requiredClauseKeys();
    String html = render(kind, required, false, true, Map.of());
    assertThat(html).contains(ENGLISH_ONLY).contains("which version prevails");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName("parties: named tenants are printed, else the signing tenants are referred to")
  void partiesTenantNames(String kind) {
    String named = render(kind, List.of("parties"), true, true, Map.of());
    assertThat(named).contains("J. Novák").doesNotContain("the tenant or tenants signing below");
    for (String empty : java.util.Arrays.asList(null, "")) {
      Map<String, Object> noNames = new HashMap<>();
      noNames.put("tenantNames", empty);
      String html = render(kind, List.of("parties"), true, true, noNames);
      assertThat(html).contains("the tenant or tenants signing below").doesNotContain("null");
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName("term: a fixed term without an end date prints the fill-in line and no end date")
  void fixedTermWithoutEndDate(String kind) {
    Map<String, Object> noEnd = new HashMap<>();
    noEnd.put("endDate", null);
    String html = render(kind, List.of("term"), true, true, noEnd);
    assertThat(html)
        .contains(
            "fixed term (nájem na dobu určitou). Term, if it does not follow from the end date")
        .doesNotContain(END_DATE)
        .doesNotContain("and ends on <strong>");
  }

  @Test
  @DisplayName("commercial deposit: an agreed deposit is printed, else a fill-in line; no cap")
  void commercialDeposit() {
    String set = render("commercial", List.of("deposit"), true, true, Map.of());
    assertThat(set).contains(DEPOSIT).doesNotContain("do not agree a security deposit");
    Map<String, Object> noDeposit = new HashMap<>();
    noDeposit.put("depositAmount", null);
    String unset = render("commercial", List.of("deposit"), true, true, noDeposit);
    assertThat(unset)
        .doesNotContain(DEPOSIT)
        .contains("do not agree a security deposit (jistota)")
        .contains("does not apply to this lease");
  }

  @Test
  @DisplayName("commercial rent: the VAT suffix appears only when the VAT clause is included")
  void commercialRentVatSuffix() {
    String suffix = "exclusive of value added tax where the lease is taxable";
    assertThat(render("commercial", List.of("rent", "vat"), true, true, Map.of())).contains(suffix);
    assertThat(render("commercial", List.of("rent"), true, true, Map.of())).doesNotContain(suffix);
  }

  @Test
  @DisplayName("commercial term: the renewal reading and its opt-out print only for a fixed term")
  void commercialRenewalOptOut() {
    String optOut = "[ ] The parties agree that the lease is not renewed in this way";
    String fixed = render("commercial", List.of("term"), true, true, Map.of());
    assertThat(fixed)
        .contains(optOut)
        .contains("under the prevailing reading of that section, section 2285")
        .contains(END_DATE);
    String indefinite = render("commercial", List.of("term"), true, false, Map.of());
    assertThat(indefinite)
        .doesNotContain(optOut)
        .doesNotContain("section 2285")
        .contains("indefinite term (nájem na dobu neurčitou)");
  }

  @Test
  @DisplayName("the Czech citation pattern takes the section with its letter and paragraph")
  void czechCitationPattern() {
    Optional<Pattern> cz = Optional.of(LeaseDocumentRegistry.CZECH_CITATION);
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>Under section 2249(1) of the Civil Code, section 2282a and section 7a(2) of"
                    + " the Act.</p>",
                cz))
        .hasSize(3)
        .anyMatch(c -> c.contains(": 2249(1)"))
        .anyMatch(c -> c.contains(": 2282a"))
        .anyMatch(c -> c.contains(": 7a(2)"));
    assertThat(LeaseDocumentFidelityTest.citations("<p>sections 2235 to 2301</p>", cz))
        .singleElement()
        .satisfies(c -> assertThat(c).contains(": 2235"));
  }
}
