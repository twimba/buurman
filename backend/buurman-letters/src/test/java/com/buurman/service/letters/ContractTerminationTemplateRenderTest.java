package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * i18n gate for {@code contract-termination-notice/generic.html}: renders it in every supported
 * document language and asserts no raw {@code termination.*}/{@code letter.*} message key leaks
 * into the output. Same technique as {@code LandlordLetterLocaleRenderTest} / {@code
 * TenantLetterLocaleRenderTest}.
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

    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames(
        "classpath:messages/document-letter-chrome",
        "classpath:messages/document-contract-termination");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messages);
  }

  @ParameterizedTest(name = "[{0}]")
  @ValueSource(
      strings = {"en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb"})
  @DisplayName("renders with no raw message-key leakage, legalClauses empty")
  void noRawKeysWithoutClauses(String locale) {
    Map<String, Object> vars = baseVariables();
    vars.put("legalClauses", List.of());
    vars.put("overrideReason", null);
    vars.put("groundLabel", null);

    String html = render(vars, locale);

    assertThat(html).as("[%s] must not leak raw i18n keys", locale).doesNotContain("termination.");
    assertThat(html)
        .as("[%s] must not leak raw letter-chrome keys", locale)
        .doesNotContain("letter.");
  }

  @Test
  @DisplayName("renders the ground and override reason when present")
  void rendersGroundAndOverrideReason() {
    Map<String, Object> vars = baseVariables();
    vars.put("legalClauses", List.of(Map.of("title", "Legal Basis", "body", "Some legal text")));
    vars.put("overrideReason", "Tenant requested an earlier move-out date");
    vars.put("groundLabel", "OWNER_OCCUPATION");

    String html = render(vars, "en");

    assertThat(html).contains("OWNER_OCCUPATION");
    assertThat(html).contains("Tenant requested an earlier move-out date");
    assertThat(html).contains("Legal Basis");
    assertThat(html).contains("Some legal text");
    assertThat(html).doesNotContain("termination.").doesNotContain("letter.");
  }

  @Test
  @DisplayName("English chrome text is the expected wording (spot check)")
  void englishChromeIsExpectedWording() {
    Map<String, Object> vars = baseVariables();
    vars.put("legalClauses", List.of());
    vars.put("overrideReason", null);
    vars.put("groundLabel", null);

    String html = render(vars, "en");

    assertThat(html).contains("Landlord");
    assertThat(html).contains("Notice of Contract Termination");
    assertThat(html).contains("Landlord / Property Manager");
  }

  private String render(Map<String, Object> vars, String locale) {
    Context context = new Context(Locale.forLanguageTag(locale));
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
    vars.put("givenByLabel", "Landlord");
    vars.put("noticeDate", "2026-01-15");
    vars.put("effectiveEndDate", "2026-04-15");
    vars.put(
        "signatureBlocks",
        List.of(
            Map.of("label", "Landlord / Property Manager", "placeholder", "signature-landlord")));
    return vars;
  }
}
