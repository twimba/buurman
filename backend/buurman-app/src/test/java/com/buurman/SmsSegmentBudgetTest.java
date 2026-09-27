package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.service.notification.channel.SmsBodyRenderer;
import com.buurman.util.DocumentLanguages;
import com.buurman.util.SmsSegment;

/**
 * Every SMS must cost exactly one segment in every language. The fixtures use deliberately long
 * values, because the budget is spent by the interpolated string, not the template.
 *
 * <p>{@code SmsBodyRenderer} ends in {@link SmsSegment#fitToOneSegment}, so a body that fits is no
 * evidence on its own — the renderer would have truncated whatever it was given. What this suite
 * actually pins is that the authored copy is short enough that truncation never fires: the
 * truncation marker must be absent from every rendered body.
 */
@DisplayName("every SMS body fits one segment in every language")
class SmsSegmentBudgetTest {

  /** {@code SmsSegment} uses an ASCII marker, so its presence means copy overflowed. */
  private static final String TRUNCATION_MARKER = "...";

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

  /** Worst-case but realistic values: a long Amsterdam address, a long personal name. */
  private static Map<String, Object> worstCaseVariables() {
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

  private static String render(String language, String templateName) {
    return renderer().render(templateName, worstCaseVariables(), Locale.forLanguageTag(language));
  }

  static Stream<Arguments> bodies() {
    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String templateName : TEMPLATE_NAMES) {
        cases.add(Arguments.of(language, templateName));
      }
    }
    return cases.stream();
  }

  @Test
  @DisplayName("the matrix covers all thirteen languages and all sixteen bodies")
  void matrixIsNotVacuous() {
    assertThat(DocumentLanguages.ORDERED).hasSize(13);
    assertThat(TEMPLATE_NAMES).hasSize(16).doesNotHaveDuplicates();
    assertThat(bodies()).hasSize(208);
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void fitsOneSegment(String language, String templateName) {
    String body = render(language, templateName);

    assertThat(body).as("body must not be blank").isNotBlank();
    assertThat(body).as("body must not leak a raw placeholder").doesNotContain("{");
    assertThat(SmsSegment.fitsOneSegment(body))
        .as(
            "[%s] %s is %d %s units, budget %d: %s",
            language,
            templateName,
            SmsSegment.unitsOf(body),
            SmsSegment.encodingOf(body),
            SmsSegment.singleSegmentBudget(body),
            body)
        .isTrue();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void copyIsShortEnoughThatTruncationNeverFires(String language, String templateName) {
    String body = render(language, templateName);

    assertThat(body)
        .as(
            "[%s] %s overflowed its %d-unit %s budget and was truncated — shorten the copy for"
                + " this language, never widen the budget: %s",
            language,
            templateName,
            SmsSegment.singleSegmentBudget(body),
            SmsSegment.encodingOf(body),
            body)
        .doesNotContain(TRUNCATION_MARKER);
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void isNotSilentlyFallingBackToTheGenericBody(String language, String templateName) {
    String generic = renderer().render("default", Map.of(), Locale.forLanguageTag(language));
    String body = render(language, templateName);

    if (!"default".equals(templateName)) {
      assertThat(body)
          .as(
              "[%s] %s fell back to the generic body — a variable is unfilled",
              language, templateName)
          .isNotEqualTo(generic);
    }
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("bodies")
  void everyLanguageActuallyHasItsOwnCopy(String language, String templateName) {
    String body = render(language, templateName);

    if (!"en".equals(language)) {
      assertThat(body)
          .as(
              "[%s] %s is byte-identical to English — the translated bundle is missing, and"
                  + " useCodeAsDefaultMessage silently served the English base file",
              language, templateName)
          .isNotEqualTo(render("en", templateName));
    }
  }
}
