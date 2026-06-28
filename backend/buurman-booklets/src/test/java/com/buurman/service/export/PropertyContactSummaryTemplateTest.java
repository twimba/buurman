package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
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

/** HTML render tests for the property + contact landscape summary-card templates. */
@DisplayName("property/contact summary templates")
class PropertyContactSummaryTemplateTest {

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

  private String render(String template, Map<String, Object> vars) {
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process(template, ctx);
  }

  @Test
  @DisplayName("property card: identity, occupancy hero, KPIs, amber rent highlight")
  void propertyCard() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("kicker", "Property Summary");
    v.put("propertyAddress", "Kerkstraat 14");
    v.put("propertyTypeLabel", "Apartment");
    v.put("propertyCategoryLabel", "Residential");
    v.put("propertyIdentifier", "P-001");
    v.put("statusCode", "OCCUPIED");
    v.put("statusLabel", "Occupied");
    v.put("heroGroundClass", "hs-occupied");
    v.put("heroStatusWord", "OCCUPIED");
    v.put("heroSizeClass", "hero-m");
    v.put("tenure", "Since 2023 → Ends 2026");
    v.put("area", "84");
    v.put("areaUnit", "m²");
    v.put("bedBath", "2 / 1");
    v.put("energyLabel", "A");
    v.put("yearBuilt", "2019");
    v.put("isVacant", false);
    v.put("headlineMoney", "€1,450.00");
    v.put("headlineUnit", "/mo");
    v.put("parking", "2 · Garage");
    v.put("energyExpiry", "Valid to 2030");
    v.put("safety", "Smoke · CO · Alarm");
    v.put("generatedMeta", "Generated 28 Jun 2026");

    String html = render("property-summary/generic", v);

    assertThat(html).contains("P-001").contains("Kerkstraat 14");
    assertThat(html).contains("Apartment").contains("Residential").contains("OCCUPIED");
    assertThat(html).contains("s-occupied").contains("hs-occupied");
    assertThat(html).contains("Area").contains("Monthly rent").contains("€1,450.00");
    assertThat(html).contains("kpi highlight");
    assertThat(html)
        .doesNotContain("summary.field.area")
        .doesNotContain("summary.field.monthlyRent");
    assertThat(html).contains("size: A4 landscape");
  }

  @Test
  @DisplayName("contact card: identity, tags, active-contracts hero, primary-channel highlight")
  void contactCard() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("kicker", "Contact Summary");
    v.put("contactName", "L. Santos");
    v.put("contactTypeLabel", "Individual");
    v.put("contactIdentifier", "CT-5001");
    v.put("tags", List.of("Tenant", "VIP"));
    v.put("activeContracts", "3");
    v.put("heroSizeClass", "hero-m");
    v.put("roleLabel", "Tenant");
    v.put("contractsCount", "5");
    v.put("propertiesCount", "2");
    v.put("lifetimePaid", "€34,800.00");
    v.put("onTimeRate", "98%");
    v.put("primaryChannelValue", "+31 6 1234 5678");
    v.put("email", "l@example.com");
    v.put("phone", "+31 6 1234 5678");
    v.put("website", null);
    v.put("generatedMeta", "Generated 28 Jun 2026");

    String html = render("contact-summary/generic", v);

    assertThat(html).contains("CT-5001").contains("L. Santos").contains("Individual");
    assertThat(html).contains("Tenant").contains("VIP"); // tags
    assertThat(html).contains("active contracts").contains("Primary contact");
    assertThat(html).contains("l@example.com").contains("kpi highlight");
    assertThat(html)
        .doesNotContain("summary.field.contracts")
        .doesNotContain("summary.contact.email");
    assertThat(html).contains("size: A4 landscape");
  }
}
