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
 * Renders the multi-page contract dossier template to HTML and asserts structure + data placement.
 * Pure template test (no DB / PDF engine); the exporter supplies pre-formatted values. Entity
 * labels live in the app bundle (off this test's classpath) and resolve to their keys here — the
 * test asserts the model binding, not the translations (parity is covered elsewhere).
 */
@DisplayName("contract-booklet dossier template")
class ContractBookletTemplateTest {

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
  @DisplayName("renders cover + details + parties + instructions + payments from the model")
  void rendersDossier() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("contractIdentifier", "C-2024-0187");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("contractTypeLabel", "Fixed term");
    v.put("propertyAddress", "Kerkstraat 14, Amsterdam");
    v.put("tenantName", "Luís Santos");
    v.put("rent", "€1,450.00");
    v.put("frequencyLabel", "Monthly");
    v.put("period", "1 Jan 2024 — 31 Dec 2024");
    v.put("startDate", "1 Jan 2024");
    v.put("endDate", "31 Dec 2024");
    v.put("deposit", "€2,900.00");
    v.put("renewalMode", "Automatic");
    v.put("propertyTypeLabel", "Apartment");
    v.put("propertyCategoryLabel", "Residential");
    v.put("rentPeriods", List.of(Map.of("from", "1 Jan 2024", "to", "—", "amount", "€1,450.00")));
    v.put("termsHtml", "<p>Private residence only.</p>");
    v.put(
        "parties",
        List.of(Map.of("role", "Primary tenant", "name", "Luís Santos", "contact", "luis@x.com")));
    // Mirror the binder: every instruction carries all keys (absent ones null).
    Map<String, Object> instr = new HashMap<>();
    instr.put("method", "Bank transfer");
    instr.put("current", true);
    instr.put("name", "Primary account");
    instr.put("bankName", null);
    instr.put("accountHolder", null);
    instr.put("iban", "NL91 ABNA 0417 1643 00");
    instr.put("bic", null);
    instr.put("reference", null);
    v.put("instructions", List.of(instr));
    v.put(
        "payments",
        List.of(
            Map.of(
                "due",
                "1 Jun",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00",
                "paid",
                "€0.00")));
    v.put("totalPaid", "€13,050.00");
    v.put("totalPending", "€1,450.00");
    v.put("totalOverdue", "€1,450.00");
    v.put("overdueCount", 1);
    v.put("generatedDate", "28 Jun 2026");

    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(v);
    String html = engine.process("contract-booklet/generic", ctx);

    // Multi-page portrait shell
    assertThat(html).contains("size: A4 portrait").contains("page-break-after");
    // Cover
    assertThat(html)
        .contains("Kerkstraat 14, Amsterdam")
        .contains("C-2024-0187")
        .contains("Fixed term");
    assertThat(html).contains("s-active"); // status badge colour class
    // Details + rent history + terms (utext)
    assertThat(html).contains("€2,900.00").contains("Automatic").contains("Apartment");
    assertThat(html).contains("<p>Private residence only.</p>"); // th:utext, not escaped
    // Parties + instructions
    assertThat(html).contains("Luís Santos").contains("NL91 ABNA 0417 1643 00");
    // Payments: overdue tile danger + status-coded row
    assertThat(html).contains("kpi danger").contains("s-overdue");
  }
}
