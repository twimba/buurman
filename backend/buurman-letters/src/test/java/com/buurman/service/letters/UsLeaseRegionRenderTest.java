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
import org.junit.jupiter.params.provider.CsvSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/**
 * The US lease documents (English only) branch per state on the first 2 letters of the contract
 * region ("CA", "NY", or a city/county form such as "NJ-HOBOKEN"), for CA, DC, MA, MD, ME, MN, NJ,
 * NY, OR and WA, with a general text for any other state, an unknown region or another country with
 * the same code. The registry gate renders every clause with a null region; this test renders each
 * state branch.
 */
@DisplayName("US lease: state branches")
class UsLeaseRegionRenderTest {

  private static final String GENERAL = "GENERAL";

  private static final List<String> STATES =
      List.of("CA", "DC", "MA", "MD", "ME", "MN", "NJ", "NY", "OR", "WA");

  /** Residential deposit clause: a distinctive phrase per state. */
  private static final Map<String, String> RESIDENTIAL_DEPOSIT =
      Map.ofEntries(
          Map.entry("CA", "No later than 21 calendar days after the tenant has vacated"),
          Map.entry("DC", "D.C. Code &sect; 42-3502.17"),
          Map.entry("MA", "a security deposit equal to the first month&rsquo;s"),
          Map.entry("MD", "Within 45 days after the end of the tenancy"),
          Map.entry("ME", "may not exceed the rent for 2 months"),
          Map.entry("MN", "within 3 weeks after the termination of the tenancy"),
          Map.entry("NJ", "may not exceed 1.5 times 1 month&rsquo;s rent"),
          Map.entry("NY", "Within 14 days after the tenant has vacated"),
          Map.entry("OR", "not later than 31 days after the tenancy terminates"),
          Map.entry("WA", "Within 30 days after the termination of this agreement"),
          Map.entry(GENERAL, "Federal law does not limit security deposits"));

  /** Residential termination clause: a distinctive phrase per state. */
  private static final Map<String, String> RESIDENTIAL_TERMINATION =
      Map.ofEntries(
          Map.entry("CA", "Civil Code &sect; 1946.1"),
          Map.entry("DC", "D.C. Code &sect; 42-3505.01"),
          Map.entry("MA", "G.L. c. 186, &sect; 12"),
          Map.entry("MD", "Real Property &sect; 8-208, subsection (d)(5)"),
          Map.entry("ME", "14 M.R.S. &sect; 6002"),
          Map.entry("MN", "Minn. Stat. &sect; 504B.135"),
          Map.entry("NJ", "N.J.S.A. 2A:18-61.2"),
          Map.entry("NY", "Good Cause Eviction Law"),
          Map.entry("OR", "ORS 90.427"),
          Map.entry("WA", "RCW 59.18.650"),
          Map.entry(GENERAL, "Several states and cities require a just cause"));

  /** Residential payment clause: a distinctive late-fee phrase per state, none in general. */
  private static final Map<String, String> RESIDENTIAL_PAYMENT =
      Map.ofEntries(
          Map.entry("CA", "treated them as liquidated damages"),
          Map.entry("DC", "D.C. Code &sect; 42-3505.31"),
          Map.entry("MA", "until 30 days after the rent was due"),
          Map.entry("MD", "5% of the amount of unpaid rent due"),
          Map.entry("ME", "4% of the amount due for 1 month"),
          Map.entry("MN", "8% of the overdue rent payment"),
          Map.entry("NJ", "grace period of 5 business days"),
          Map.entry("NY", "USD 50 or 5% of the monthly rent"),
          Map.entry("OR", "by the 4th day of the rental period"),
          Map.entry("WA", "RCW 59.18.170"));

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
      String deposit,
      Map<String, Integer> refs) {
    Map<String, Object> clause = new HashMap<>();
    clause.put("clauseKey", clauseKey);
    clause.put("title", "TITLE-" + clauseKey);
    clause.put("articleNumber", 1);
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/US/" + kind + "/en");
    vars.put("authoritative", true);
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", "en");
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("landlordName", "Example Landlord LLC");
    vars.put("tenantNames", "A. Smith");
    vars.put("propertyAddress", "1 Main Street, Springfield");
    vars.put("startDate", "1 November 2026");
    vars.put("endDate", "31 October 2027");
    vars.put("fixedTerm", fixedTerm);
    vars.put("rentAmount", "$2,000.00");
    vars.put("paymentFrequency", "monthly");
    vars.put("paymentDueDay", 1);
    vars.put("depositAmount", deposit);
    vars.put("landlordNoticeDays", 30);
    vars.put("tenantNoticeDays", 30);
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", refs);
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  private String render(String kind, String clauseKey, String country, String region) {
    return render(kind, clauseKey, country, region, false, "$2,000.00", Map.of());
  }

  /** The state the documents branch on: the first 2 letters before a hyphen, US only. */
  private static String expectedKey(String country, String region) {
    if (!"US".equals(country) || region == null) {
      return GENERAL;
    }
    String state = region.split("-", 2)[0];
    return STATES.contains(state) ? state : GENERAL;
  }

  /** Residential rent-adjustment clause: a distinctive phrase per state; MA has none. */
  private static final Map<String, String> RESIDENTIAL_RENT_ADJUSTMENT =
      Map.ofEntries(
          Map.entry("CA", "Civil Code &sect; 827"),
          Map.entry("DC", "D.C. Code &sect; 42-3509.04"),
          Map.entry("MD", "Prince George&rsquo;s County"),
          Map.entry("ME", "City of Portland"),
          Map.entry("MN", "City of Saint Paul"),
          Map.entry("NJ", "N.J.S.A. 2A:18-61.1"),
          Map.entry("NY", "Real Property Law &sect; 226-c"),
          Map.entry("OR", "ORS 90.324"),
          Map.entry("WA", "RCW 59.18.720"),
          Map.entry(GENERAL, "Some states and cities limit the amount or frequency"));

  /** Residential disclosures clause: state notices; DC and WA get the general text. */
  private static final Map<String, String> RESIDENTIAL_DISCLOSURES =
      Map.ofEntries(
          Map.entry("CA", "www.meganslaw.ca.gov"),
          Map.entry("MA", "G.L. c. 111, &sect; 197"),
          Map.entry("MD", "Maryland Tenants&rsquo; Bill of Rights"),
          Map.entry("ME", "14 M.R.S. &sect; 6030-D"),
          Map.entry("MN", "Minn. Stat. &sect; 504B.195"),
          Map.entry("NJ", "Truth in Renting statement"),
          Map.entry("NY", "Real Property Law &sect; 231-a"),
          Map.entry("OR", "ORS 90.228"),
          Map.entry(GENERAL, "Many states and cities require further disclosures"));

  /** Residential entry clause: state notice rules; other states only the common text. */
  private static final Map<String, String> RESIDENTIAL_ENTRY =
      Map.ofEntries(
          Map.entry("CA", "Civil Code &sect; 1954"),
          Map.entry("MA", "G.L. c. 186, &sect; 15B, subsection (1)(a)"),
          Map.entry("ME", "14 M.R.S. &sect; 6025"),
          Map.entry("MN", "Minn. Stat. &sect; 504B.211"),
          Map.entry("OR", "ORS 90.322"),
          Map.entry("WA", "RCW 59.18.150"));

  /** Residential maintenance clause: state habitability rules. */
  private static final Map<String, String> RESIDENTIAL_MAINTENANCE =
      Map.ofEntries(
          Map.entry("CA", "Civil Code &sect; 1941.1"),
          Map.entry("MD", "Real Property &sect; 8-208, subsection (c)(1)"),
          Map.entry("MN", "Minn. Stat. &sect; 504B.161"),
          Map.entry("NY", "Real Property Law &sect; 235-b"),
          Map.entry("WA", "RCW 59.18.060"));

  /** Residential handover-inspection clause: state inspection rules. */
  private static final Map<String, String> RESIDENTIAL_HANDOVER =
      Map.ofEntries(
          Map.entry("CA", "subdivision (g)(2)"),
          Map.entry("MA", "subsection (2)(c)"),
          Map.entry("MD", "Real Property &sect; 8-203"),
          Map.entry("MN", "Minn. Stat. &sect; 504B.182"),
          Map.entry("NY", "General Obligations Law &sect; 7-108"),
          Map.entry("WA", "RCW 59.18.260"));

  private static void assertOnlyBranch(
      String html, Map<String, String> phrases, String expectedState) {
    // a state without its own branch in this clause gets the general text, where there is one
    String expected =
        phrases.containsKey(expectedState) || !phrases.containsKey(GENERAL)
            ? expectedState
            : GENERAL;
    assertThat(html).doesNotContain("${").doesNotContain("null");
    if (phrases.containsKey(expected)) {
      assertThat(html).contains(phrases.get(expected));
    }
    phrases.forEach(
        (key, phrase) -> {
          if (!key.equals(expected)) {
            assertThat(html).as("branch %s must not render", key).doesNotContain(phrase);
          }
        });
  }

  @ParameterizedTest(name = "{0}/{1}")
  @CsvSource(
      value = {
        "US, CA",
        "US, DC",
        "US, MA",
        "US, MD",
        "US, ME",
        "US, MN",
        "US, NJ",
        "US, NY",
        "US, OR",
        "US, WA",
        "US, NJ-HOBOKEN",
        "US, MN-STPAUL",
        "US, MD-TAKOMAPARK",
        "US, ME-PORTLAND",
        "US, NJ-JERSEYCITY",
        "US, TX",
        "US, NULL",
        "CA, ON",
        "GB, NY"
      },
      nullValues = "NULL")
  @DisplayName("residential state clauses print exactly the state's branch")
  void residentialBranches(String country, String region) {
    String expected = expectedKey(country, region);
    assertOnlyBranch(
        render("residential", "rent-adjustment", country, region),
        RESIDENTIAL_RENT_ADJUSTMENT,
        expected);
    assertOnlyBranch(
        render("residential", "disclosures", country, region), RESIDENTIAL_DISCLOSURES, expected);
    assertOnlyBranch(render("residential", "entry", country, region), RESIDENTIAL_ENTRY, expected);
    assertOnlyBranch(
        render("residential", "maintenance", country, region), RESIDENTIAL_MAINTENANCE, expected);
    assertOnlyBranch(
        render("residential", "handover-inspection", country, region),
        RESIDENTIAL_HANDOVER,
        expected);
    assertOnlyBranch(
        render("residential", "deposit", country, region), RESIDENTIAL_DEPOSIT, expected);
    assertOnlyBranch(
        render("residential", "payment", country, region), RESIDENTIAL_PAYMENT, expected);
    assertOnlyBranch(
        render("residential", "termination", country, region), RESIDENTIAL_TERMINATION, expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      value = {"CA", "DC", "MA", "MD", "ME", "MN", "NJ", "NY", "OR", "WA", "TX", "NULL"},
      nullValues = "NULL")
  @DisplayName("every residential and commercial clause renders cleanly in every state branch")
  void everyClauseRendersInEveryState(String region) {
    for (String kind : List.of("residential", "commercial")) {
      LeaseDocumentRegistry.Entry entry =
          LeaseDocumentRegistry.ENTRIES.stream()
              .filter(e -> e.countryCode().equals("US"))
              .filter(e -> e.kind().name().toLowerCase(Locale.ROOT).equals(kind))
              .findFirst()
              .orElseThrow();
      for (String key : entry.clauseKeys()) {
        for (boolean fixedTerm : List.of(true, false)) {
          for (String deposit : new String[] {"$2,000.00", null}) {
            String html = render(kind, key, "US", region, fixedTerm, deposit, Map.of());
            assertThat(html)
                .as("%s/%s %s fixed=%s", kind, key, region, fixedTerm)
                .contains("data-clause=\"" + key + "\"")
                .doesNotContain("${")
                .doesNotContainPattern("(?<!\\p{L})null(?!\\p{L})");
          }
        }
      }
    }
  }

  @Test
  @DisplayName("the general premises text names no state; a known state gets the state sentence")
  void premisesGeneralAndState() {
    assertThat(render("residential", "premises", "US", null))
        .contains("The rules of the state in which the dwelling is located are not set out")
        .doesNotContain("selected rules of the state");
    assertThat(render("residential", "premises", "US", "OR"))
        .contains("selected rules of the state")
        .doesNotContain("are not set out in this agreement");
  }

  @Test
  @DisplayName("residential disclosures: federal lead warning everywhere, state notices only there")
  void residentialDisclosures() {
    String general = render("residential", "disclosures", "US", null);
    assertThat(general)
        .contains("Housing built before 1978 may contain lead-based paint.")
        .contains("Many states and cities require further disclosures")
        .contains("Agent&rsquo;s statement")
        .contains("subsections (b)(5), (b)(6) and (c)")
        .doesNotContain("www.meganslaw.ca.gov")
        .doesNotContain("Real Property Law &sect; 231-a");
    assertThat(render("residential", "disclosures", "US", "CA"))
        .contains("Housing built before 1978 may contain lead-based paint.")
        .contains("www.meganslaw.ca.gov")
        .contains("Government Code &sect; 8589.45")
        .doesNotContain("Many states and cities require further disclosures");
    assertThat(render("residential", "disclosures", "US", "NY"))
        .contains("Real Property Law &sect; 231-a")
        .contains("National Flood Insurance Program (NFIP)")
        .doesNotContain("www.meganslaw.ca.gov");
  }

  @Test
  @DisplayName("residential term: fixed term prints the end date, periodic does not")
  void residentialTerm() {
    String fixed = render("residential", "term", "US", "CA", true, null, Map.of());
    assertThat(fixed).contains("The tenancy is for a fixed term").contains("31 October 2027");
    String periodic = render("residential", "term", "US", "CA", false, null, Map.of());
    assertThat(periodic)
        .contains("The tenancy is periodic (month-to-month)")
        .doesNotContain("31 October 2027");
  }

  @Test
  @DisplayName("residential deposit: the amount is printed when set, a fill-in line otherwise")
  void residentialDepositAmount() {
    assertThat(render("residential", "deposit", "US", "NY", false, "$2,000.00", Map.of()))
        .contains("$2,000.00");
    assertThat(render("residential", "deposit", "US", "NY", false, null, Map.of()))
        .contains("the amount agreed between the parties: ____");
  }

  @Test
  @DisplayName("commercial: California-only CASp, operating cost and jury waiver text")
  void commercialCaliforniaBranches() {
    String caAccess = render("commercial", "accessibility", "US", "CA");
    assertThat(caAccess)
        .contains("28 CFR &sect; 36.201")
        .contains("A Certified Access Specialist (CASp) can inspect the subject premises");
    assertThat(render("commercial", "accessibility", "US", "NY"))
        .contains("28 CFR &sect; 36.201")
        .doesNotContain("Certified Access Specialist");
    assertThat(render("commercial", "service-costs", "US", "CA"))
        .contains("Civil Code &sect; 1950.9");
    assertThat(render("commercial", "deposit", "US", "CA")).contains("Civil Code &sect; 1950.7");
    assertThat(render("commercial", "deposit", "US", "NY"))
        .doesNotContain("Civil Code &sect; 1950.7");
    assertThat(render("residential", "termination", "US", "OR"))
        .contains("4 or fewer residential dwelling units");
    assertThat(render("commercial", "service-costs", "US", "TX"))
        .doesNotContain("Civil Code &sect; 1950.9");
    assertThat(render("commercial", "disputes", "US", "CA"))
        .contains("does not apply to premises in California");
    assertThat(render("commercial", "disputes", "US", null))
        .doesNotContain("does not apply to premises in California");
    assertThat(render("commercial", "termination", "US", "CA", false, null, Map.of()))
        .contains("Civil Code &sect; 1946.1");
    assertThat(render("commercial", "termination", "US", "CA", true, null, Map.of()))
        .doesNotContain("Civil Code &sect; 1946.1");
  }

  @Test
  @DisplayName("commercial term: fixed term with end date, or periodic tenancy")
  void commercialTerm() {
    assertThat(render("commercial", "term", "US", null, true, null, Map.of()))
        .contains("The lease is for a fixed term")
        .contains("31 October 2027");
    assertThat(render("commercial", "term", "US", null, false, null, Map.of()))
        .contains("periodic (month-to-month) tenancy")
        .doesNotContain("31 October 2027");
  }

  @Test
  @DisplayName("cross-references sit outside the state branches and render for every state")
  void crossReferencesRenderForEveryState() {
    for (String region : List.of("CA", "NY", "WA", "TX")) {
      String html =
          render(
              "residential",
              "deposit",
              "US",
              region,
              false,
              "$2,000.00",
              Map.of("handover-inspection", 7));
      assertThat(html)
          .as(region)
          .contains("data-ref=\"handover-inspection\"")
          .contains("article <span>7</span>");
    }
  }

  private static List<String> tokens(String text) {
    List<String> found = new ArrayList<>();
    Matcher m = LeaseDocumentRegistry.US_CITATION.matcher(text);
    while (m.find()) {
      found.add(m.group());
    }
    return found;
  }

  @Test
  @DisplayName("the US citation pattern keeps dotted, hyphen and letter suffixes in the token")
  void usCitationPattern() {
    assertThat(
            tokens(
                "Civil Code § 1950.5, § 2079.10a, General Obligations Law § 7-108, Real Property"
                    + " Law § 235-b, Minn. Stat. § 504B.178, 42 U.S.C. § 4852d, c. 186 section 15B,"
                    + " ORS 90.323, RCW 59.18.280, N.J.S.A. 46:8-21.2, 14 M.R.S. § 6030-D"))
        .containsExactly(
            "1950.5",
            "2079.10a",
            "7-108",
            "235-b",
            "504B.178",
            "4852d",
            "15B",
            "90.323",
            "59.18.280",
            "46:8-21.2",
            "6030-D");
  }
}
