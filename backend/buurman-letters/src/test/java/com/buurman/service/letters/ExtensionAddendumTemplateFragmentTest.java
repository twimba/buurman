package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/**
 * Pure template-level smoke test (no exporter/Spring wiring): renders {@code
 * extension-addendum/generic.html} with the real Thymeleaf engine to prove its {@code
 * th:replace="~{_letter-styles :: sig-placeholder-css}"} fragment reference resolves, after the
 * template's own inline copy of {@code .sig-placeholder} was removed in favor of the shared
 * fragment.
 */
class ExtensionAddendumTemplateFragmentTest {

  @Test
  void rendersWithTheSharedSigPlaceholderFragmentResolved() {
    MessageSource messageSource =
        DocumentTemplateSupport.messageSource(false, "classpath:messages/document-extension");
    TemplateEngine engine = DocumentTemplateSupport.templateEngine(messageSource, false);

    Map<String, Object> vars = new HashMap<>();
    vars.put("extensionNumber", 1);
    vars.put("extensionIdentifier", "EXT00000000000000000000001");
    vars.put("extensionStatus", "ACTIVE");
    vars.put("extensionStatusDisplay", "Active");
    vars.put("propertyAddress", "Keizersgracht 12, Amsterdam");
    vars.put("propertyFullAddress", "Keizersgracht 12, Amsterdam");
    vars.put("hasMultipleUnits", false);
    vars.put("unitDesignation", null);
    vars.put("primaryContactName", "Jan de Vries");
    vars.put("contactNames", "Jan de Vries");
    vars.put("contractStartDate", "1 January 2026");
    vars.put("contractEndDate", "Indefinite");
    vars.put("contractType", "Fixed Term");
    vars.put("previousEndDate", "1 January 2027");
    vars.put("newEndDate", "1 January 2028");
    vars.put("previousRent", "1,500.00");
    vars.put("newRent", "1,600.00");
    vars.put("triggerType", "Manual");
    vars.put("adjustmentType", "Fixed Amount");
    vars.put("adjustmentValue", "100.00");
    vars.put("activatedDate", "—");
    vars.put("notes", null);
    vars.put("legalClauses", List.of());
    vars.put("landlordSignaturePlaceholder", "signature-landlord");
    vars.put("tenantSignaturePlaceholder", "signature-tenant-1");
    vars.put("contractIdentifier", "CON00000000000000000000001");
    vars.put("generatedDate", "1 March 2026");

    Context context = new Context(java.util.Locale.ENGLISH, vars);
    String html = engine.process("extension-addendum/generic", context);

    assertThat(html).contains("sig-placeholder");
    assertThat(html).doesNotContain("th:replace");
  }
}
