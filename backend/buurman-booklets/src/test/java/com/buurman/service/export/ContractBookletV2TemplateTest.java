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

/** HTML render test for the redesigned multi-page contract booklet (v2). */
@DisplayName("contract-booklet-v2 template")
class ContractBookletV2TemplateTest {

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
  @DisplayName("renders cover + details + parties + payments across paged sections")
  void rendersBooklet() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("propertyAddress", "Kerkstraat 14");
    v.put("contractIdentifier", "C-2024-0187");
    v.put("contractType", "Fixed-term tenancy");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("landlordName", "Vastgoed Bakker B.V.");
    v.put("tenantName", "Luís Santos");
    v.put("rent", "€1,450.00");
    v.put("deposit", "€2,900.00");
    v.put("frequencyLabel", "Monthly");
    v.put("termRange", "1 Jan 2024 → 31 Dec 2024");
    v.put("totalPaid", "€13,050.00");
    v.put("totalPending", "€1,450.00");
    v.put("totalOverdue", "€1,450.00");
    v.put("overdueCount", 1);
    v.put(
        "parties",
        List.of(Map.of("role", "Primary tenant", "name", "Luís Santos", "contact", "luis@x.com")));
    v.put(
        "payments",
        List.of(
            Map.of(
                "date",
                "1 Jun",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00")));

    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(v);
    String html = engine.process("contract-booklet-v2/generic", ctx);

    assertThat(html).contains("Kerkstraat 14").contains("C-2024-0187").contains("Fixed-term");
    assertThat(html).contains("Contract details").contains("Parties").contains("Payment overview");
    assertThat(html).contains("Luís Santos").contains("Primary tenant");
    assertThat(html).contains("€13,050.00").contains("kpi danger"); // overdue tile
    assertThat(html).contains("s-overdue"); // status-coded payment row
    assertThat(html).contains("page-break-after").contains("size: A4 portrait");
    assertThat(html).doesNotContain("booklet.page.details").doesNotContain("summary.field.rent");
  }
}
