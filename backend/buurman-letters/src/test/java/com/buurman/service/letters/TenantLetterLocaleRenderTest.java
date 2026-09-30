package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * i18n gate for the tenant-facing letters (formal notice of overdue rent, deposit statement):
 * renders both templates in every supported document language and asserts that no raw {@code
 * notice.*}/{@code deposit.*}/{@code letter.*} message key leaks into the output.
 */
@DisplayName("tenant letter multi-locale render gate")
class TenantLetterLocaleRenderTest {

  private static final List<String> LOCALES =
      List.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  private SpringTemplateEngine engine;
  private ReloadableResourceBundleMessageSource messages;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames(
        "classpath:messages/document-letter-chrome",
        "classpath:messages/document-extension",
        "classpath:messages/document-payment-notice",
        "classpath:messages/document-deposit-statement");
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(true);
    messages = ms;
    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
  }

  static Stream<Arguments> templatesAndLocales() {
    List<String> templates = List.of("payment-formal-notice/generic", "deposit-statement/generic");
    return templates.stream().flatMap(t -> LOCALES.stream().map(l -> Arguments.of(t, l)));
  }

  @ParameterizedTest(name = "{0} [{1}]")
  @MethodSource("templatesAndLocales")
  @DisplayName("renders with no raw message-key leakage")
  void noRawKeys(String template, String locale) {
    Context ctx = new Context(Locale.forLanguageTag(locale));
    ctx.setVariables(allVars());
    String html = engine.process(template, ctx);

    assertThat(html)
        .as("%s [%s] must not leak raw i18n keys", template, locale)
        .doesNotContain("notice.")
        .doesNotContain("deposit.")
        .doesNotContain("letter.")
        .contains("EUR 1,250.00")
        .contains("ctr_01TEST");

    // The title must come from this locale's own bundle, not fall back to English.
    String titleKey =
        template.startsWith("payment-formal-notice") ? "notice.title" : "deposit.title";
    Locale loc = Locale.forLanguageTag(locale);
    String title = messages.getMessage(titleKey, null, loc);
    assertThat(html).contains(title);
    if (!locale.equals("en")) {
      assertThat(title).isNotEqualTo(messages.getMessage(titleKey, null, Locale.ENGLISH));
    }
  }

  private static Map<String, Object> allVars() {
    Map<String, Object> v = new HashMap<>();
    v.put("generatedDate", "24 Sep 2026");
    v.put("paymentIdentifier", "pay_01TEST");
    v.put("depositIdentifier", "dep_01TEST");
    v.put("contractIdentifier", "ctr_01TEST");
    v.put("primaryContactName", "Alex Tenant");
    v.put(
        "contactAddress",
        Map.of(
            "street", "Main St 1", "postalCode", "1000", "city", "Amsterdam", "countryCode", "NL"));
    v.put("propertyAddress", "Canal 2, Amsterdam");
    v.put("amount", "EUR 1,250.00");
    v.put("received", "EUR 250.00");
    v.put("hasPartialPayment", true);
    v.put("outstanding", "EUR 1,000.00");
    v.put("dueDate", "1 Sep 2026");
    v.put("daysOverdue", 23);
    v.put("deadline", "8 Oct 2026");
    v.put("deadlineDays", 14);
    v.put("hasInstructions", true);
    v.put("iban", "NL00BANK0123456789");
    v.put("accountHolderName", "Landlord BV");
    v.put("paymentReference", "ctr_01TEST");
    v.put("countryCode", "NL");
    v.put("legalClauses", List.of(Map.of("body", "Clause text")));
    v.put("contractStart", "1 Jan 2025");
    v.put("contractEnd", "31 Dec 2026");
    v.put("status", "HELD");
    v.put("depositAmount", "EUR 1,250.00");
    v.put("receivedDate", "1 Jan 2025");
    v.put("heldWhere", "Escrow account");
    v.put("returnDueDate", "31 Jan 2027");
    v.put(
        "deductions",
        List.of(Map.of("date", "15 Sep 2026", "reason", "Wall repair", "amount", "EUR 100.00")));
    v.put("hasDeductions", true);
    v.put("deductionsTotal", "EUR 100.00");
    v.put("hasReturned", false);
    v.put("returnedAmount", "EUR 0.00");
    v.put("returnedDate", null);
    v.put("refundable", "EUR 1,150.00");
    v.put("isClosed", false);
    v.put("notes", "Thanks for keeping the flat tidy.");
    return v;
  }
}
