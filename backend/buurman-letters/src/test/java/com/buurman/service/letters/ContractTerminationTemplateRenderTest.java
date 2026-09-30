package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders {@code contract-termination-notice/generic.html} directly (bypassing the exporter, same
 * technique as {@code LetterTemplateServiceTest}) to catch template syntax errors or unresolved
 * variables that a mocked-exporter unit test can't.
 */
@DisplayName("contract-termination-notice template render")
class ContractTerminationTemplateRenderTest {

  private SpringTemplateEngine engine;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
  }

  @Test
  @DisplayName("renders the core notice fields when there is no country legal clause")
  void rendersWithoutLegalClause() {
    Map<String, Object> vars = baseVariables();
    vars.put("legalClauses", List.of());
    vars.put("overrideReason", null);
    vars.put("groundLabel", null);

    String html = render(vars);

    assertThat(html).contains("LANDLORD");
    assertThat(html).contains("2026-01-15");
    assertThat(html).contains("2026-04-15");
    assertThat(html).doesNotContain("legal-clause-title");
  }

  @Test
  @DisplayName("renders the ground and override reason when present")
  void rendersGroundAndOverrideReason() {
    Map<String, Object> vars = baseVariables();
    vars.put("legalClauses", List.of(Map.of("title", "Legal Basis", "body", "Some legal text")));
    vars.put("overrideReason", "Tenant requested an earlier move-out date");
    vars.put("groundLabel", "OWNER_OCCUPATION");

    String html = render(vars);

    assertThat(html).contains("OWNER_OCCUPATION");
    assertThat(html).contains("Tenant requested an earlier move-out date");
    assertThat(html).contains("Legal Basis");
    assertThat(html).contains("Some legal text");
  }

  private String render(Map<String, Object> vars) {
    Context context = new Context(Locale.ENGLISH);
    context.setVariables(vars);
    return engine.process("contract-termination-notice/generic", context);
  }

  private Map<String, Object> baseVariables() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("generatedDate", "2026-01-15");
    vars.put("contractIdentifier", "CON00000000000000000000001");
    vars.put("primaryContactName", "Jane Tenant");
    vars.put("contactAddress", null);
    vars.put("propertyAddress", "Dorpsstraat 5, 1234 AB Amsterdam");
    vars.put("hasMultipleUnits", false);
    vars.put("unitDesignation", null);
    vars.put("givenByLabel", "LANDLORD");
    vars.put("noticeDate", "2026-01-15");
    vars.put("effectiveEndDate", "2026-04-15");
    return vars;
  }
}
