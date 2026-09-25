package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

@DisplayName("EmailSubjectResolver")
class EmailSubjectResolverTest {

  private static final String TEMPLATE = "payment-reminder-tenant";

  private EmailSubjectResolver resolver(Map<String, String> messages) {
    StaticMessageSource source = new StaticMessageSource();
    source.setUseCodeAsDefaultMessage(false);
    messages.forEach((code, msg) -> source.addMessage(code, Locale.ENGLISH, msg));
    return new EmailSubjectResolver(source);
  }

  private Map<String, Object> variables(String tone) {
    return Map.of("propertyName", "12 Example Street", "tone", tone);
  }

  @Test
  @DisplayName("each reminder tone gets its own subject line")
  void toneVariantWins() {
    EmailSubjectResolver resolver =
        resolver(
            Map.of(
                "email.subject." + TEMPLATE, "Rent reminder for {0}",
                "email.subject." + TEMPLATE + ".FIRM", "Overdue rent for {0}: action needed",
                "email.subject." + TEMPLATE + ".FINAL", "Final notice: overdue rent for {0}"));

    assertThat(resolver.resolve(TEMPLATE, variables("FIRM"), Locale.ENGLISH))
        .isEqualTo("Overdue rent for 12 Example Street: action needed");
    assertThat(resolver.resolve(TEMPLATE, variables("FINAL"), Locale.ENGLISH))
        .isEqualTo("Final notice: overdue rent for 12 Example Street");
  }

  @Test
  @DisplayName(
      "a locale without the tone variant falls back to the generic subject, not the default")
  void fallsBackToGenericSubject() {
    EmailSubjectResolver resolver =
        resolver(
            Map.of(
                "email.subject." + TEMPLATE,
                "Rent reminder for {0}",
                "email.subject.default",
                "Notification"));

    assertThat(resolver.resolve(TEMPLATE, variables("FIRM"), Locale.ENGLISH))
        .isEqualTo("Rent reminder for 12 Example Street");
  }

  @Test
  @DisplayName("other templates are unaffected by the tone lookup")
  void otherTemplatesUnchanged() {
    EmailSubjectResolver resolver =
        resolver(Map.of("email.subject.contract-expiry", "Contract for {0} expires soon"));

    assertThat(resolver.resolve("contract-expiry", variables("FIRM"), Locale.ENGLISH))
        .isEqualTo("Contract for 12 Example Street expires soon");
  }
}
