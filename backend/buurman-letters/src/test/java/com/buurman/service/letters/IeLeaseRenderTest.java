package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.LeaseKind;

/**
 * The IE lease documents (English only, no region branches): the residential and commercial term
 * and deposit clauses print the right text for a fixed term and for a periodic tenancy, with and
 * without a deposit amount, and every clause renders cleanly in both cases. The registry-driven
 * render gate renders English-only entries only with an end date and a deposit; this test covers
 * the other combinations.
 */
@DisplayName("IE lease: term and deposit variants")
class IeLeaseRenderTest {

  private static final String END_DATE = "31 October 2027";
  private static final String DEPOSIT = "EUR 1,500.00";

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

  private String render(String kind, List<String> keys, boolean fixedTerm, boolean withDeposit) {
    return render(kind, keys, fixedTerm, withDeposit, Map.of());
  }

  private String render(
      String kind,
      List<String> keys,
      boolean fixedTerm,
      boolean withDeposit,
      Map<String, Object> overrides) {
    List<Map<String, Object>> clauses = new ArrayList<>();
    Map<String, Integer> refs = new HashMap<>();
    for (int i = 0; i < keys.size(); i++) {
      Map<String, Object> clause = new HashMap<>();
      clause.put("clauseKey", keys.get(i));
      clause.put("title", "TITLE-" + keys.get(i));
      clause.put("articleNumber", i + 1);
      clauses.add(clause);
      refs.put(keys.get(i), i + 1);
    }
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/IE/" + kind + "/en");
    vars.put("authoritative", true);
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", "en");
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord Ltd");
    vars.put("tenantNames", "A. Murphy");
    vars.put("propertyAddress", "1 Main Street, Dublin 2");
    vars.put("startDate", "1 November 2026");
    vars.put("endDate", fixedTerm ? END_DATE : null);
    vars.put("fixedTerm", fixedTerm);
    vars.put("rentAmount", "EUR 1,450.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("paymentDueDay", null);
    vars.put("depositAmount", withDeposit ? DEPOSIT : null);
    vars.put("countryCode", "IE");
    vars.put("regionCode", null);
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    vars.putAll(overrides);
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private static List<String> keys(LeaseKind kind) {
    return LeaseDocumentRegistry.find("IE", kind).orElseThrow().clauseKeys();
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName("every clause renders without template residue, periodic and without a deposit")
  void periodicWithoutDeposit(String kind) {
    LeaseKind leaseKind = "residential".equals(kind) ? LeaseKind.RESIDENTIAL : LeaseKind.COMMERCIAL;
    String html = render(kind, keys(leaseKind), false, false);
    assertThat(html)
        .doesNotContain("${")
        .doesNotContain("??")
        .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})")
        .doesNotContain(END_DATE)
        .doesNotContain(DEPOSIT)
        .contains("deposit in the amount agreed in writing");
    for (String key : keys(leaseKind)) {
      assertThat(html).contains("data-clause=\"" + key + "\"");
    }
  }

  @Test
  @DisplayName(
      "residential term: periodic tenancy, or fixed term that does not end a Part 4 tenancy")
  void residentialTerm() {
    String periodic = render("residential", List.of("term"), false, true);
    assertThat(periodic)
        .contains("The tenancy is a periodic tenancy")
        .doesNotContain("fixed term")
        .contains("tenancy of minimum duration (section 35B(1) of the Act)");
    String fixed = render("residential", List.of("term"), true, true);
    assertThat(fixed)
        .contains("granted for a fixed term")
        .contains("<strong>" + END_DATE + "</strong>")
        .contains("does not by itself end a Part 4 tenancy")
        .doesNotContain("The tenancy is a periodic tenancy");
  }

  @Test
  @DisplayName("residential deposit: amount printed and capped at 1 month's rent")
  void residentialDeposit() {
    String html = render("residential", List.of("deposit"), true, true);
    assertThat(html)
        .contains("pays a deposit of <strong>" + DEPOSIT + "</strong>")
        .contains("in respect of a period of 1 month")
        .doesNotContain("amount agreed in writing");
  }

  @Test
  @DisplayName("commercial term: fixed term with end date, or periodic tenancy ended by notice")
  void commercialTerm() {
    assertThat(render("commercial", List.of("term"), true, true))
        .contains("granted for a fixed term")
        .contains("<strong>" + END_DATE + "</strong>")
        .doesNotContain("periodic tenancy");
    assertThat(render("commercial", List.of("term"), false, true))
        .contains("periodic tenancy from rent period to rent period")
        .doesNotContain("fixed term");
  }

  @Test
  @DisplayName("commercial deposit: amount printed, residential limits stated not to apply")
  void commercialDeposit() {
    assertThat(render("commercial", List.of("deposit"), true, true))
        .contains("rent deposit of <strong>" + DEPOSIT + "</strong>")
        .contains("do not apply to this lease");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName("parties without tenant names print the signing-tenant wording")
  void partiesWithoutTenantNames(String kind) {
    String html = render(kind, List.of("parties"), true, true, Map.of("tenantNames", ""));
    assertThat(html)
        .contains("the tenant or tenants signing below")
        .doesNotContain("A. Murphy")
        .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"residential", "commercial"})
  @DisplayName("a fixed term without an end date renders without a date and without residue")
  void fixedTermWithoutEndDate(String kind) {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("endDate", null);
    String html = render(kind, List.of("term"), true, true, overrides);
    assertThat(html)
        .contains("granted for a fixed term")
        .doesNotContain("ending on")
        .doesNotContain(END_DATE)
        .doesNotContain("${")
        .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})");
  }

  @Test
  @DisplayName("the Irish citation pattern takes the section, Part or paragraph number")
  void irishCitationPattern() {
    String text =
        "section 35B(1) of the Act, sections 20(1) and 20B(2), Part 4, s.19B, paragraph 3 of the"
            + " Table, regulation 6";
    List<String> found = new ArrayList<>();
    Matcher m = LeaseDocumentRegistry.IRISH_CITATION.matcher(text);
    while (m.find()) {
      found.add(m.group());
    }
    assertThat(found).containsExactly("35B", "20", "4", "19B", "3", "6");
  }
}
