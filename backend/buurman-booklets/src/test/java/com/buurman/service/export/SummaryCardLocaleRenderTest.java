package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * i18n gate: renders every summary-card template in a representative locale set (incl. non-Latin el
 * and long-compound de/fi) and asserts no raw {@code summary.*}/{@code booklet.*} message key leaks
 * into the output — i.e. every {@code #{...}} a template references actually exists in the bundle.
 * (Full visual-regression/one-page-per-locale checks still need a Gotenberg-capable CI job.)
 */
@DisplayName("summary-card multi-locale render gate")
class SummaryCardLocaleRenderTest {

  private SpringTemplateEngine engine;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/document-summary-card");
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(
        true); // a missing key would surface as the raw key — caught below
    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
  }

  static Stream<Arguments> templatesAndLocales() {
    List<String> templates =
        List.of("contract-summary/generic", "property-summary/generic", "contact-summary/generic");
    List<String> locales = List.of("en", "de", "el", "fi");
    return templates.stream().flatMap(t -> locales.stream().map(l -> Arguments.of(t, l)));
  }

  @ParameterizedTest(name = "{0} [{1}]")
  @MethodSource("templatesAndLocales")
  @DisplayName("renders with no raw message-key leakage")
  void noRawKeys(String template, String locale) {
    Context ctx = new Context(Locale.forLanguageTag(locale));
    ctx.setVariables(allVars());
    String html = engine.process(template, ctx);

    assertThat(html)
        .as("%s [%s] must not leak raw i18n keys", template, locale)
        .doesNotContain("summary.")
        .doesNotContain("booklet.");
  }

  /** A superset of the variables the three templates read (extra keys are ignored per template). */
  private static Map<String, Object> allVars() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("generatedDate", "29 Jun 2026");
    v.put("qrDataUri", null);
    // contract
    v.put("propertyAddress", "Kerkstraat 14");
    v.put("contractIdentifier", "C-1");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("landlordName", "Bakker B.V.");
    v.put("tenantName", "L. Santos");
    v.put("heroSizeClass", "hero-m");
    v.put("rent", "€1.450,00");
    v.put("deposit", "€2.900,00");
    v.put("frequencyLabel", "Vierteljährlich");
    v.put("termLabel", "Fixed term");
    v.put("nextPaymentAmount", "€1.450,00");
    v.put("nextPaymentWhen", "1 Jul 2026");
    v.put("nextPaymentDanger", false);
    v.put("paidPct", 70);
    v.put("pendingPct", 20);
    v.put("overduePct", 10);
    v.put("paidCount", 7);
    v.put("pendingCount", 2);
    v.put("overdueCount", 1);
    v.put("leaseStart", "1 Jan 2024");
    v.put("leaseEnd", "31 Dec 2024");
    v.put("termElapsedPct", 42);
    v.put("termElapsedLabel", "42%");
    // property
    v.put("propertyTypeLabel", "Apartment");
    v.put("propertyCategoryLabel", "Residential");
    v.put("propertyIdentifier", "P-1");
    v.put("heroGroundClass", "hs-occupied");
    v.put("heroStatusWord", "OCCUPIED");
    v.put("tenure", "Since 2023");
    v.put("area", "84");
    v.put("areaUnit", "m²");
    v.put("bedBath", "2 / 1");
    v.put("energyLabel", "A");
    v.put("yearBuilt", "2019");
    v.put("isVacant", false);
    v.put("headlineMoney", "€1.450,00");
    v.put("parking", "1 · Garage");
    v.put("energyExpiry", "2030");
    v.put("renovated", "2021");
    // contact
    v.put("contactName", "L. Santos");
    v.put("contactTypeLabel", "Individual");
    v.put("contactIdentifier", "CT-1");
    v.put("tags", List.of("Tenant"));
    v.put("activeContracts", "3");
    v.put("roleLabel", "Primary tenant");
    v.put("contractsCount", "5");
    v.put("propertiesCount", "2");
    v.put("lifetimePaid", "€34.800,00");
    v.put("onTimeRate", "98%");
    v.put("primaryChannelValue", "+31 6 12 34 56 78");
    v.put("email", "l@example.com");
    v.put("phone", "+31 6 12 34 56 78");
    v.put("website", null);
    v.put("mailingAddress", "Kerkstraat 14");
    v.put("taxId", "NL0000.00.000.B01");
    return v;
  }
}
