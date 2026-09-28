package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.config.models.TwilioProperties;
import com.buurman.service.MetricsService;
import com.buurman.service.notification.RenderedContent;
import com.buurman.util.DocumentLanguages;
import com.buurman.util.SmsSegment;

/**
 * {@link TwilioSmsSender} is {@code @Profile("!local")}, so it is never instantiated by the rest of
 * the suite — which runs under the local profile — and it is the sender that actually costs money
 * per segment in production. Both senders previously carried byte-identical hardcoded English
 * {@code switch} blocks and dropped the {@code Locale} they were handed; this pins that they now
 * delegate to the same renderer and agree exactly.
 */
@DisplayName("both SMS senders render identically")
class SmsSenderParityTest {

  private static final List<String> TEMPLATE_NAMES =
      List.of(
          "welcome",
          "verification-code",
          "phone-verification-code",
          "team-invitation",
          "invitation-accepted",
          "password-changed",
          "payment-reminder",
          "contract-expiry",
          "property-created",
          "contract-created",
          "contract-status-changed",
          "contract-reopened",
          "payment-paid",
          "payment-receival",
          "expense-created",
          "default");

  private static SmsBodyRenderer renderer() {
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/sms-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);
    return new SmsBodyRenderer(messages);
  }

  private static LocalSmsSender localSender() {
    return new LocalSmsSender(mock(MetricsService.class), renderer());
  }

  /** Construction alone is worth pinning: the constructor gained a parameter in this work. */
  private static TwilioSmsSender twilioSender() {
    TwilioProperties properties =
        new TwilioProperties(
            "AC00000000000000000000000000000000",
            "test-auth-token",
            "+15550000000",
            Optional.empty(),
            Optional.empty());
    return new TwilioSmsSender(properties, mock(MetricsService.class), renderer());
  }

  private static Map<String, Object> variables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", "Alexandra Wilhelmina");
    variables.put("baseUrl", "https://app.buurman.io");
    variables.put("verificationCode", "483920");
    variables.put("expiresMinutes", "15");
    variables.put("inviterName", "Alexandra Wilhelmina");
    variables.put("memberName", "Alexandra Wilhelmina");
    variables.put("teamName", "Amsterdam Grachtengordel Vastgoed");
    variables.put("amount", "EUR 1.250,00");
    variables.put("receivalAmount", "EUR 1.250,00");
    variables.put("propertyName", "Keizersgracht 123-B, Amsterdam");
    variables.put("contactName", "Alexandra Wilhelmina");
    variables.put("dueDate", "15/10/2026");
    variables.put("expiryDate", "15/10/2026");
    variables.put("daysUntilExpiry", "30");
    variables.put("oldStatus", "Active");
    variables.put("newStatus", "Terminated");
    variables.put("category", "Maintenance");
    return variables;
  }

  static Stream<Arguments> matrix() {
    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String templateName : TEMPLATE_NAMES) {
        cases.add(Arguments.of(language, templateName));
      }
    }
    return cases.stream();
  }

  @Test
  @DisplayName("the Twilio sender can be constructed and reports the SMS channel")
  void twilioSenderIsConstructible() {
    TwilioSmsSender sender = twilioSender();

    assertThat(sender.getChannel()).isEqualTo(SMS);
    assertThat(localSender().getChannel()).isEqualTo(SMS);
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("matrix")
  @DisplayName("agree exactly, in every language")
  void sendersAgree(String language, String templateName) {
    Locale locale = Locale.forLanguageTag(language);

    RenderedContent viaTwilio = twilioSender().render(templateName, variables(), locale);
    RenderedContent viaLocal = localSender().render(templateName, variables(), locale);

    assertThat(viaTwilio.body()).isEqualTo(viaLocal.body());
    assertThat(viaTwilio.channel()).isEqualTo(SMS);
    assertThat(viaTwilio.subject()).isEmpty();
    assertThat(SmsSegment.fitsOneSegment(viaTwilio.body())).isTrue();
  }

  @Test
  @DisplayName("the Twilio sender honours the locale it is given rather than dropping it")
  void twilioSenderHonoursTheLocale() {
    TwilioSmsSender sender = twilioSender();

    String english = sender.render("payment-reminder", variables(), Locale.ENGLISH).body();
    String portuguese =
        sender.render("payment-reminder", variables(), Locale.forLanguageTag("pt")).body();
    String greek =
        sender.render("payment-reminder", variables(), Locale.forLanguageTag("el")).body();

    assertThat(portuguese).isNotEqualTo(english);
    assertThat(greek).isNotEqualTo(english).isNotEqualTo(portuguese);
    assertThat(english).contains("overdue");
    assertThat(portuguese).contains("atraso");
  }
}
