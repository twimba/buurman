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

/** Renders the multi-page contact dossier template and asserts structure + data placement. */
@DisplayName("contact-booklet dossier template")
class ContactBookletTemplateTest {

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
  @DisplayName("renders cover + profile + addresses + rentals + payments + notes + relationships")
  void rendersDossier() {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("contactName", "Luís Santos");
    v.put("contactTypeLabel", "Individual");
    v.put("contactIdentifier", "CT-5001");
    v.put("isIndividual", true);
    v.put("email", "luis@example.com");
    v.put("phone", "+31 6 12 34 56 78");
    v.put("website", null);
    v.put("companyName", null);
    v.put("tradeName", null);
    v.put("industry", null);
    v.put("dateOfBirth", "4 Mar 1989");
    v.put("taxNumber", "NL0000.00.000.B01");
    v.put("idNumber", null);
    v.put("currentProperty", "Kerkstraat 14, Amsterdam");
    v.put("tags", List.of("Tenant", "VIP"));
    v.put("profileNotesHtml", "<p>Reliable payer.</p>");
    v.put("totalPaid", "€34,800.00");
    v.put("outstanding", "€1,450.00");
    v.put("hasOutstanding", true);
    v.put("activeContracts", "1");
    v.put("totalContracts", "2");
    v.put(
        "addresses",
        List.of(
            Map.of(
                "type",
                "Current",
                "active",
                true,
                "street",
                "Kerkstraat 14",
                "city",
                "Amsterdam",
                "country",
                "NL")));
    v.put(
        "rentals",
        List.of(
            Map.of(
                "role",
                "Primary tenant",
                "status",
                "Active",
                "statusCode",
                "ACTIVE",
                "property",
                "Kerkstraat 14",
                "contractId",
                "C-2024-0187",
                "type",
                "Fixed term",
                "start",
                "1 Jan 2024",
                "end",
                "Ongoing",
                "rent",
                "€1,450.00",
                "frequency",
                "Monthly")));
    v.put(
        "payments",
        List.of(
            Map.of(
                "due",
                "1 Jun",
                "paidOn",
                "—",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00")));
    Map<String, Object> note = new HashMap<>();
    note.put("type", "Call");
    note.put("pinned", true);
    note.put("date", "12 May 2026");
    note.put("author", "A. Bakker");
    note.put("subject", "Annual review");
    note.put("body", "Discussed indexation.");
    v.put("notes", List.of(note));
    v.put(
        "relationships",
        List.of(Map.of("name", "Maria Santos", "type", "Spouse", "notes", "Co-occupant")));
    v.put("generatedDate", "28 Jun 2026");

    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(v);
    String html = engine.process("contact-booklet/generic", ctx);

    assertThat(html).contains("size: A4 portrait").contains("page-break-after");
    assertThat(html).contains("Luís Santos").contains("CT-5001").contains("Individual");
    assertThat(html).contains("Tenant").contains("VIP"); // tags
    assertThat(html).contains("<p>Reliable payer.</p>"); // profile notes utext
    assertThat(html).contains("€34,800.00").contains("kpi danger"); // outstanding tile
    assertThat(html).contains("s-active"); // rental status badge
    assertThat(html).contains("s-overdue"); // payment status badge
    assertThat(html)
        .contains("Discussed indexation.")
        .contains("Maria Santos"); // notes + relationships
  }
}
