package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the landscape contract summary-card template to HTML and asserts structure + i18n chrome.
 * Pure template test — no DB, no PDF engine; the binder supplies pre-formatted values.
 */
@DisplayName("contract-summary template")
class ContractSummaryTemplateTest {

  private SpringTemplateEngine engine;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:messages/document-summary-card");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setUseCodeAsDefaultMessage(true);

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messageSource);
  }

  private static Map<String, Object> sampleVars() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("kicker", "Tenancy Summary");
    v.put("propertyAddress", "Kerkstraat 14, Amsterdam");
    v.put("contractIdentifier", "C-2024-001");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("landlordName", "A. Bakker");
    v.put("tenantName", "L. Santos");
    v.put("heroSizeClass", "hero-m");
    v.put("rent", "€1,450.00");
    v.put("rentPeriodUnit", "/mo");
    v.put("deposit", "€2,900.00");
    v.put("frequencyLabel", "Monthly");
    v.put("termLabel", "12 months");
    v.put("nextPaymentAmount", "€1,450.00");
    v.put("nextPaymentWhen", "1 Jul");
    v.put("nextPaymentDanger", false);
    v.put("paidPct", 70);
    v.put("pendingPct", 20);
    v.put("overduePct", 10);
    v.put("paidCount", 7);
    v.put("pendingCount", 2);
    v.put("overdueCount", 1);
    v.put("paymentInstruction", "NL00 BANK 0123 4567 89");
    v.put("qrDataUri", null);
    v.put("generatedDate", "Generated 28 Jun 2026");
    return v;
  }

  private String render(Locale locale, Map<String, Object> vars) {
    Context ctx = new Context(locale);
    ctx.setVariables(vars);
    return engine.process("contract-summary/generic", ctx);
  }

  @Test
  @DisplayName("renders identity, route, KPIs and status with no unresolved message keys")
  void rendersCard() {
    String html = render(Locale.ENGLISH, sampleVars());

    // Data
    assertThat(html).contains("C-2024-001");
    assertThat(html).contains("Kerkstraat 14, Amsterdam");
    assertThat(html).contains("A. Bakker").contains("L. Santos");
    assertThat(html).contains("€1,450.00").contains("Monthly").contains("12 months");
    // Status badge code drives the colour class
    assertThat(html).contains("Active").contains("s-active");
    // i18n chrome resolved from the bundle (not raw keys)
    assertThat(html).contains("Landlord").contains("Tenant").contains("Your portfolio, in focus.");
    assertThat(html).doesNotContain("summary.field.rent").doesNotContain("summary.tagline");
    // Landscape page + brand bands present
    assertThat(html).contains("size: A4 landscape").contains("brand-band").contains("brand-footer");
  }

  @Test
  @DisplayName("amber highlight when next payment is on time, red when overdue")
  void highlightVariant() {
    assertThat(render(Locale.ENGLISH, sampleVars())).contains("kpi highlight");

    Map<String, Object> overdue = sampleVars();
    overdue.put("nextPaymentDanger", true);
    assertThat(render(Locale.ENGLISH, overdue)).contains("kpi danger");
  }
}
