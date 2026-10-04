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
 * The CH residential rent clause branches on the canton: per the BWO list for 2026 the initial-rent
 * form (Art. 270 Abs. 2 OR) is compulsory throughout BS, BE, FR, GE, LU, ZG and ZH and in part of
 * NE and VD; any other or unknown canton, or another country with the same region code (BE is also
 * Belgium), prints the general paragraph. All 4 languages carry the same branch structure (fidelity
 * gate).
 */
@DisplayName("CH lease: cantonal initial-rent form branch")
class ChLeaseRegionRenderTest {

  enum Branch {
    CANTON_WIDE,
    PARTIAL,
    GENERAL
  }

  private static final Map<Branch, Map<String, String>> MARKERS =
      Map.of(
          Branch.CANTON_WIDE,
          Map.of(
              "de", "im ganzen Kantonsgebiet",
              "fr", "sur l'ensemble du territoire cantonal",
              "it", "sull'intero territorio cantonale",
              "en", "applies throughout the canton"),
          Branch.PARTIAL,
          Map.of(
              "de", "nur in einem Teil des Kantonsgebiets",
              "fr", "ne s'applique que sur une partie du territoire cantonal",
              "it", "soltanto in una parte del territorio cantonale",
              "en", "applies only in part of the canton"),
          Branch.GENERAL,
          Map.of(
              "de", "Im Falle von Wohnungsmangel können die Kantone",
              "fr", "En cas de pénurie de logements, les cantons",
              "it", "In caso di penuria di abitazioni, i Cantoni",
              "en", "In the event of a housing shortage, the cantons"));

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
    vars.put("clauseSource", "lease-agreement/CH/residential/" + language);
    vars.put("authoritative", language.equals("de"));
    vars.put("fallbackUsed", false);
    vars.put("requestedLang", language);
    vars.put("generatedDate", "4 October 2026");
    vars.put("contractIdentifier", "CON01TEST");
    vars.put("rentComponents", List.of());
    vars.put("signatureBlocks", List.of());
    vars.put("rentAmount", "CHF 2,100.00");
    vars.put("paymentFrequency", "per month");
    vars.put("countryCode", countryCode);
    vars.put("regionCode", regionCode);
    vars.put("clauses", List.of(clause));
    vars.put("refs", Map.of("rent", 1));
    Context ctx = new Context(Locale.forLanguageTag(language));
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx).replaceAll("\\s+", " ");
  }

  @ParameterizedTest(name = "{0} {1}/{2} -> {3}")
  @CsvSource(
      value = {
        "de, CH, ZH, CANTON_WIDE",
        "fr, CH, GE, CANTON_WIDE",
        "it, CH, BE, CANTON_WIDE",
        "en, CH, BS, CANTON_WIDE",
        "de, CH, LU, CANTON_WIDE",
        "fr, CH, FR, CANTON_WIDE",
        "en, CH, ZG, CANTON_WIDE",
        "de, CH, VD, PARTIAL",
        "fr, CH, NE, PARTIAL",
        "it, CH, VD, PARTIAL",
        "en, CH, NE, PARTIAL",
        "de, CH, VS, GENERAL",
        "fr, CH, NULL, GENERAL",
        "it, CH, TI, GENERAL",
        "en, CH, NULL, GENERAL",
        "de, BE, BE, GENERAL",
        "en, LU, LU, GENERAL"
      },
      nullValues = "NULL")
  @DisplayName("canton-wide, partial or general initial-rent form paragraph, exactly one of them")
  void branchesOnCanton(String language, String country, String region, Branch expected) {
    String html = renderRent(language, country, region);
    assertThat(html).contains("data-clause=\"rent\"").doesNotContain("${");
    for (Branch branch : Branch.values()) {
      String marker = MARKERS.get(branch).get(language);
      if (branch == expected) {
        assertThat(html).as("%s present", branch).contains(marker);
      } else {
        assertThat(html).as("%s absent", branch).doesNotContain(marker);
      }
    }
    if (expected != Branch.GENERAL) {
      assertThat(html).as("canton code printed").contains("<strong>" + region + "</strong>");
    }
  }
}
