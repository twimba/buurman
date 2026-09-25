package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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
 * The dunning ladder lets a landlord pick a tone per step, so the tones must actually produce
 * different emails. They previously did not: the template branched only on whether the payment was
 * overdue, which made a FIRM step byte-identical to a FRIENDLY one once rent was late.
 */
@DisplayName("tenant reminder tones render differently")
class TenantReminderToneRenderTest {

  private SpringTemplateEngine engine;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/email/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/email-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messages);
  }

  private String render(String tone, boolean overdue, Locale locale) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("contactName", "Sam Example");
    vars.put("propertyName", "12 Example Street");
    vars.put("teamName", "Acme Rentals");
    vars.put("amount", "EUR 1,200.00");
    vars.put("received", "EUR 0.00");
    vars.put("outstanding", "EUR 1,200.00");
    vars.put("hasPartialPayment", false);
    vars.put("dueDate", "1 September 2026");
    vars.put("daysOverdue", overdue ? 14 : 0);
    vars.put("isOverdue", overdue);
    vars.put("tone", tone);
    vars.put("isFinal", "FINAL".equals(tone));
    vars.put("notes", "");
    vars.put("baseUrl", "https://app.test");
    vars.put("primaryUrl", "https://app.test/payments");
    vars.put("hasInstructions", false);
    Context ctx = new Context(locale, vars);
    return engine.process("payment-reminder-tenant", ctx);
  }

  @Test
  @DisplayName("friendly, firm and final produce three different emails for the same arrears")
  void tonesDiffer() {
    String friendly = render("FRIENDLY", true, Locale.ENGLISH);
    String firm = render("FIRM", true, Locale.ENGLISH);
    String last = render("FINAL", true, Locale.ENGLISH);

    assertThat(friendly).isNotEqualTo(firm);
    assertThat(firm).isNotEqualTo(last);
    assertThat(friendly).isNotEqualTo(last);
  }

  @Test
  @DisplayName("only the firm step warns about fees and collection")
  void firmWarnsAboutConsequences() {
    assertThat(render("FIRM", true, Locale.ENGLISH))
        .contains("Rent still outstanding")
        .contains("formal collection steps");
    assertThat(render("FRIENDLY", true, Locale.ENGLISH)).doesNotContain("formal collection steps");
  }

  @Test
  @DisplayName("a friendly step before the due date reads as a courtesy, not as arrears")
  void friendlyBeforeDueIsNotOverdue() {
    assertThat(render("FRIENDLY", false, Locale.ENGLISH))
        .contains("friendly reminder")
        .doesNotContain("overdue");
  }

  @ParameterizedTest(name = "tones differ in {0}")
  @ValueSource(strings = {"nl", "de", "fr", "es", "pt", "it", "sv", "fi", "el", "pl", "da", "nb"})
  @DisplayName("every locale has its own firm wording, not an English fallback")
  void tonesDifferPerLocale(String language) {
    Locale locale = Locale.forLanguageTag(language);
    String friendly = render("FRIENDLY", true, locale);
    String firm = render("FIRM", true, locale);

    assertThat(firm).isNotEqualTo(friendly);
    // A missing key would surface as the raw code thanks to useCodeAsDefaultMessage.
    assertThat(firm).doesNotContain("email.payment-reminder-tenant.");
    assertThat(firm).isNotEqualTo(render("FIRM", true, Locale.ENGLISH));
  }
}
