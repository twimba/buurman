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

/**
 * Renders the multi-page property dossier template to HTML and asserts structure + data placement.
 * Pure template test (no DB / PDF engine); the exporter supplies pre-formatted, list-driven values.
 * Static labels live in the app bundle (off this test's classpath) and resolve to their keys here —
 * the test asserts the model binding, not the translations (parity is covered elsewhere).
 */
@DisplayName("property-booklet dossier template")
class PropertyBookletTemplateTest {

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
    ms.setUseCodeAsDefaultMessage(true);

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
  }

  @Test
  @DisplayName(
      "renders cover + overview + specs + features + safety + photos + financials + contracts +"
          + " dashboard")
  void rendersDossier() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("identifier", "P-0001");
    v.put("street", "Kerkstraat 14");
    v.put("location", "Amsterdam, 1012 AB, NL");
    v.put("propertyTypeLabel", "Apartment");
    v.put("statusCode", "OCCUPIED");
    v.put("statusLabel", "Occupied");
    v.put(
        "coverRows",
        List.of(
            Map.of("k", "Bedrooms / Bathrooms", "v", "3 bed / 2 bath"),
            Map.of("k", "Total area", "v", "120 sqm"),
            Map.of("k", "Year built", "v", "1998")));
    v.put(
        "detailsFields",
        List.of(
            Map.of("l", "Property type", "v", "Apartment"),
            Map.of("l", "Status", "v", "Occupied"),
            Map.of("l", "Total area", "v", "120 sqm")));
    v.put("constructionFields", List.of(Map.of("l", "Construction type", "v", "Brick")));
    v.put("categoryTitle", "Residential details");
    v.put("categoryFields", List.of(Map.of("l", "Bedrooms", "v", "3")));
    v.put("structuralNotes", "Renovated roof in 2020.");

    v.put("hasBuildingSpecs", true);
    v.put("energyRating", "A");
    v.put("energyColor", "#059669");
    v.put("energyFields", List.of(Map.of("l", "Heating system", "v", "District heating")));
    v.put("insulationNotes", "Triple glazing throughout.");
    v.put("utilitiesFields", List.of(Map.of("l", "Electricity", "v", "Grid")));
    v.put("parkingFields", List.of(Map.of("l", "Parking spaces", "v", "1")));

    v.put(
        "amenityGroups",
        List.of(Map.of("category", "Indoor", "items", List.of("Dishwasher", "Balcony"))));
    v.put("outdoorAreas", List.of(Map.of("type", "Garden", "area", "45 sqm")));
    v.put("hasFeatures", true);

    v.put("hasSafety", true);
    v.put(
        "safetyChecks",
        List.of(
            Map.of("label", "Smoke detectors", "ok", true),
            Map.of("label", "Sprinkler system", "ok", false)));
    v.put("safetyNotes", "Inspected annually.");
    v.put("accessibilityChecks", List.of(Map.of("label", "Elevator", "ok", true)));
    v.put("accessibilityNotes", null);

    v.put(
        "photos",
        List.of(
            Map.of("src", "data:image/jpeg;base64,AAAA", "label", "Front facade", "main", true)));

    v.put(
        "financialYears",
        List.of(
            Map.of(
                "year",
                "2024",
                "income",
                "€18,000.00",
                "expenses",
                "€4,200.00",
                "net",
                "€13,800.00")));

    v.put(
        "contracts",
        List.of(
            Map.of(
                "id", "C-2024-0187",
                "contact", "Luís Santos",
                "start", "1 Jan 2024",
                "end", "Ongoing",
                "rent", "€1,450.00",
                "statusCode", "ACTIVE",
                "statusLabel", "Active")));

    v.put("hasDashboard", true);
    v.put(
        "dashMetrics",
        List.of(Map.of("l", "Total ROI", "v", "12.5%"), Map.of("l", "Occupancy", "v", "100%")));
    Map<String, Object> cf = new HashMap<>();
    cf.put("month", "Jan 2024");
    cf.put("income", "€1,500.00");
    cf.put("expenses", "€350.00");
    cf.put("mortgage", "€600.00");
    cf.put("net", "€550.00");
    cf.put("net_positive", true);
    v.put("cashFlow", List.of(cf));
    v.put("expenseBreakdown", List.of(Map.of("category", "Maintenance", "amount", "€1,200.00")));
    v.put("equityRows", List.of(Map.of("l", "Purchase price", "v", "€420,000.00")));

    v.put("qrDataUri", "data:image/svg+xml;base64,QQ==");
    v.put("generatedDate", "30 Jun 2026");

    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(v);
    String html = engine.process("property-booklet/generic", ctx);

    // Multi-page portrait shell
    assertThat(html).contains("size: A4 portrait").contains("page-break-after");
    // Cover
    assertThat(html)
        .contains("Kerkstraat 14")
        .contains("Amsterdam, 1012 AB, NL")
        .contains("P-0001")
        .contains("Apartment");
    assertThat(html).contains("s-occupied"); // status badge colour class
    assertThat(html).contains("3 bed / 2 bath"); // category-specific cover row
    // Overview + construction + category + structural notes
    assertThat(html)
        .contains("Brick")
        .contains("Residential details")
        .contains("Renovated roof in 2020.");
    // Building specs: energy grade chip + utilities + parking
    assertThat(html)
        .contains("#059669")
        .contains("District heating")
        .contains("Triple glazing throughout.");
    // Features
    assertThat(html).contains("Dishwasher").contains("Balcony").contains("Garden");
    // Safety: a checked + an unchecked mark
    assertThat(html)
        .contains("Smoke detectors")
        .contains("✓")
        .contains("✕")
        .contains("Inspected annually.");
    // Photos (base64 + main tag)
    assertThat(html).contains("data:image/jpeg;base64,AAAA").contains("Front facade");
    // Financials
    assertThat(html).contains("€18,000.00").contains("€13,800.00");
    // Contracts table
    assertThat(html).contains("C-2024-0187").contains("Luís Santos").contains("s-active");
    // Dashboard
    assertThat(html).contains("12.5%").contains("Maintenance").contains("€420,000.00");
  }

  @Test
  @DisplayName("renders when optional sub-sections are absent (null notes / empty checks)")
  void rendersWithSparseOptionalSections() {
    // Reproduces the SpEL `or`-with-null-operand crash: a property that has safety data but NO
    // accessibility data (empty checks + null notes), and building specs present only via
    // utilities (null energyRating, empty energyFields, null insulationNotes). The compound
    // th:if conditions must not try to coerce a null/String operand to boolean.
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("identifier", "P-0009");
    v.put("street", "Lange Voorhout 1");
    v.put("location", "Den Haag, NL");
    v.put("propertyTypeLabel", "Office");
    v.put("statusCode", "VACANT");
    v.put("statusLabel", "Vacant");
    v.put("coverRows", List.of(Map.of("k", "Total area", "v", "300 sqm")));
    v.put("detailsFields", List.of(Map.of("l", "Status", "v", "Vacant")));
    v.put("constructionFields", List.of());
    v.put("categoryTitle", null);
    v.put("categoryFields", List.of());
    v.put("structuralNotes", null);

    v.put("hasBuildingSpecs", true);
    v.put("energyRating", null);
    v.put("energyColor", "#78716c");
    v.put("energyFields", List.of());
    v.put("insulationNotes", null);
    v.put("utilitiesFields", List.of(Map.of("l", "Water", "v", "Mains")));
    v.put("parkingFields", List.of());

    v.put("amenityGroups", List.of());
    v.put("outdoorAreas", List.of());
    v.put("hasFeatures", false);

    v.put("hasSafety", true);
    v.put("safetyChecks", List.of(Map.of("label", "Smoke detectors", "ok", true)));
    v.put("safetyNotes", null);
    v.put("accessibilityChecks", List.of());
    v.put("accessibilityNotes", null);

    v.put("photos", List.of());
    v.put("financialYears", List.of());
    v.put("contracts", List.of());

    v.put("hasDashboard", false);
    v.put("dashMetrics", List.of());
    v.put("cashFlow", List.of());
    v.put("expenseBreakdown", List.of());
    v.put("equityRows", List.of());

    v.put("qrDataUri", "data:image/svg+xml;base64,QQ==");
    v.put("generatedDate", "30 Jun 2026");

    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(v);

    // Must not throw (the bug raised EL1001E: cannot convert from null to boolean).
    String html = engine.process("property-booklet/generic", ctx);
    assertThat(html).contains("Lange Voorhout 1").contains("Smoke detectors").contains("Mains");
    // Accessibility sub-section is fully absent (no checks, no notes).
    assertThat(html).doesNotContain("Elevator");
  }
}
