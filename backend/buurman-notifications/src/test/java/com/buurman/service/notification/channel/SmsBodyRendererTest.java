package com.buurman.service.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.util.SmsSegment;

@DisplayName("SmsBodyRenderer")
class SmsBodyRendererTest {

  private SmsBodyRenderer renderer;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/sms-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);
    renderer = new SmsBodyRenderer(messages);
  }

  private Map<String, Object> paymentReminderVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("amount", "EUR 1.250,00");
    variables.put("propertyName", "Keizersgracht 123-B");
    variables.put("dueDate", "15/10/2026");
    return variables;
  }

  @Test
  @DisplayName("interpolates the named variables")
  void interpolatesVariables() {
    String body = renderer.render("payment-reminder", paymentReminderVariables(), Locale.ENGLISH);

    assertThat(body)
        .isEqualTo(
            "Buurman: Payment of EUR 1.250,00 for Keizersgracht 123-B is overdue (due"
                + " 15/10/2026).");
  }

  @Test
  @DisplayName("an unknown template falls back to the generic body")
  void unknownTemplateFallsBack() {
    String body = renderer.render("no-such-template", Map.of(), Locale.ENGLISH);

    assertThat(body).isEqualTo("Buurman: You have a new notification.");
  }

  @Test
  @DisplayName("never delivers a raw placeholder when a variable is missing")
  void missingVariableFallsBackRatherThanLeakingAPlaceholder() {
    Map<String, Object> incomplete = new HashMap<>();
    incomplete.put("amount", "EUR 1.250,00");

    String body = renderer.render("payment-reminder", incomplete, Locale.ENGLISH);

    assertThat(body).doesNotContain("{");
    assertThat(body).isEqualTo("Buurman: You have a new notification.");
  }

  @Test
  @DisplayName("null variables do not blow up")
  void nullVariablesAreTolerated() {
    String body = renderer.render("password-changed", null, Locale.ENGLISH);

    assertThat(body)
        .isEqualTo("Buurman: Your password was changed. Contact support if unexpected.");
  }

  @Test
  @DisplayName("always returns a body that fits one segment")
  void alwaysFitsOneSegment() {
    Map<String, Object> variables = paymentReminderVariables();
    variables.put("propertyName", "P".repeat(300));

    String body = renderer.render("payment-reminder", variables, Locale.ENGLISH);

    assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
  }
}
