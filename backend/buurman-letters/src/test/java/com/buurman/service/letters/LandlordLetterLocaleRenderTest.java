package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * i18n gate for the landlord-initiated letters (rent-increase, extension addendum, rent-change):
 * renders each in every supported document language and asserts no raw message key leaks into the
 * output. Also proves the per-country-addenda feature's two named cases (DE's two-section §558
 * clause, NL's one-section legal-basis clause) render with the right structure and order.
 */
@DisplayName("landlord letter multi-locale render gate")
class LandlordLetterLocaleRenderTest {

  private static final List<String> LOCALES =
      List.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  private SpringTemplateEngine engine;
  private ReloadableResourceBundleMessageSource messages;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames(
        "classpath:messages/document-letter-chrome",
        "classpath:messages/document-extension",
        "classpath:messages/document-rent-change");
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(true);
    messages = ms;
    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
  }

  static Stream<Arguments> templatesAndLocales() {
    List<String> templates =
        List.of(
            "rent-increase-letter/generic", "extension-addendum/generic", "rent-change/generic");
    return templates.stream().flatMap(t -> LOCALES.stream().map(l -> Arguments.of(t, l)));
  }

  @ParameterizedTest(name = "{0} [{1}]")
  @MethodSource("templatesAndLocales")
  @DisplayName("renders with no raw message-key leakage, legalClauses empty")
  void noRawKeysWithoutClauses(String template, String locale) {
    Context ctx = new Context(Locale.forLanguageTag(locale));
    ctx.setVariables(allVars(List.of()));
    String html = engine.process(template, ctx);

    assertThat(html)
        .as("%s [%s] must not leak raw i18n keys", template, locale)
        .doesNotContain("letter.");
  }

  @Test
  @DisplayName("DE two-clause catalog entry renders both sections in order, with headings")
  void deTwoClauseCatalogEntryRendersInOrder() {
    List<Map<String, String>> clauses =
        List.of(
            Map.of("title", "Section 558 Justification", "body", "Body one"),
            Map.of("title", "Comparison Method", "body", "Body two"));
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(allVars(clauses));
    String html = engine.process("rent-increase-letter/generic", ctx);

    assertThat(html)
        .containsSubsequence(
            "Section 558 Justification", "Body one", "Comparison Method", "Body two");
  }

  @Test
  @DisplayName(
      "a single title-less clause (the legacy fallback shape) renders the body with no empty"
          + " heading")
  void singleTitlelessClauseRendersBodyOnly() {
    List<Map<String, String>> clauses = List.of(Map.of("body", "Legacy clause text"));
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(allVars(clauses));
    String html = engine.process("rent-increase-letter/generic", ctx);

    assertThat(html).contains("Legacy clause text").doesNotContain("legal-clause-title");
  }

  private static Map<String, Object> allVars(List<Map<String, String>> legalClauses) {
    Map<String, Object> v = new HashMap<>();
    v.put("generatedDate", "24 Sep 2026");
    v.put("extensionIdentifier", "cex_01TEST");
    v.put("extensionNumber", 1);
    v.put("contractIdentifier", "ctr_01TEST");
    v.put("primaryContactName", "Alex Tenant");
    v.put(
        "contactAddress",
        Map.of(
            "street", "Main St 1", "postalCode", "1000", "city", "Amsterdam", "countryCode", "NL"));
    v.put("propertyAddress", "Canal 2, Amsterdam");
    v.put("hasMultipleUnits", false);
    v.put("unitDesignation", null);
    v.put("previousRent", "EUR 1,000.00");
    v.put("newRent", "EUR 1,050.00");
    v.put("rentEffectiveDate", "1 Nov 2026");
    v.put("adjustmentType", "Fixed Percentage");
    v.put("adjustmentBasis", "a 5% increase");
    v.put("adjustmentValue", "5%");
    v.put("newEndDate", null);
    v.put("isInitialRent", false);
    v.put("effectiveDate", "1 Nov 2026");
    v.put("rentPeriodIdentifier", "rnp_01TEST");
    v.put("countryCode", "DE");
    v.put("legalClauses", legalClauses);
    // rent-increase-letter / rent-change read signatureBlocks; extension-addendum reads these
    // two directly instead (its signature table is a fixed landlord|tenant pair, not a loop).
    v.put(
        "signatureBlocks",
        List.of(
            Map.of("label", "Landlord / Property Manager", "placeholder", "signature-landlord")));
    v.put("landlordSignaturePlaceholder", "signature-landlord");
    v.put("tenantSignaturePlaceholder", "signature-tenant-1");
    return v;
  }
}
