package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.buurman.service.notification.EmailSubjectResolver;
import com.buurman.util.DocumentLanguages;

/**
 * Every email template must render in every language. Templates are discovered from disk rather
 * than listed, so a new one is covered the day it is added.
 */
@DisplayName("every email template renders in every language")
class EmailRenderMatrixTest {

  /**
   * Resolved from the classpath, not the working directory: a CWD-relative path works under
   * Surefire but throws NoSuchFileException from an IDE run at the repo root, where the failure
   * looks like a template-discovery bug rather than a path problem.
   */
  private static Path templateDir() throws IOException {
    try {
      URL location = EmailRenderMatrixTest.class.getClassLoader().getResource("templates/email");
      if (location == null) {
        throw new IOException("templates/email is not on the classpath");
      }
      return Paths.get(location.toURI());
    } catch (URISyntaxException e) {
      throw new IOException("templates/email resolved to an unusable URI", e);
    }
  }

  /**
   * A bare message key leaked into the output, e.g. {@code email.welcome.greeting}. Needs two
   * dotted segments so ordinary prose ("check your email. Then...") cannot trip it.
   */
  private static final Pattern MESSAGE_KEY = Pattern.compile("email\\.[a-z0-9-]+\\.[a-zA-Z0-9_-]+");

  private static ReloadableResourceBundleMessageSource messages() {
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames(
        "classpath:messages/email-subjects",
        "classpath:messages/email-bodies",
        "classpath:messages/sms-bodies");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    messages.setUseCodeAsDefaultMessage(true);
    return messages;
  }

  private static SpringTemplateEngine engine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/email/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messages());
    return engine;
  }

  /**
   * A superset of every variable any template references. Templates take what they need and ignore
   * the rest, so one map beats twenty-four hand-maintained fixtures.
   */
  private static Map<String, Object> variables() {
    Map<String, Object> variables = new HashMap<>();
    List<String> textual =
        List.of(
            "accountHolderName",
            "activatedBy",
            "additionalDetails",
            "amount",
            "bankName",
            "baseUrl",
            "bicSwift",
            "category",
            "contactName",
            "contractIdentifier",
            "ctaText",
            "ctaUrl",
            "description",
            "dueDate",
            "effectiveFrom",
            "endDate",
            "expiresAt",
            "expiresMinutes",
            "expiryDate",
            "followUpDate",
            "iban",
            "introText",
            "invitationCode",
            "inviterName",
            "inviteUrl",
            "memberEmail",
            "memberName",
            "newEndDate",
            "newRentAmount",
            "newRentFormatted",
            "newStatus",
            "notes",
            "noteSubject",
            "oldRentAmount",
            "oldStatus",
            "outstanding",
            "paymentDate",
            "paymentReference",
            "previousEndDate",
            "previousRentFormatted",
            "primaryText",
            "primaryUrl",
            "propertyAddress",
            "propertyName",
            "propertyType",
            "receivalAmount",
            "received",
            "recipientName",
            "registerUrl",
            "remainingBalance",
            "renewalMode",
            "rentAmount",
            "rentChangeFormatted",
            "role",
            "secondaryText",
            "secondaryUrl",
            "senderName",
            "startDate",
            "teamName",
            "triggerType",
            "typeName",
            "userName",
            "verificationCode",
            "verifyUrl");
    for (String name : textual) {
      variables.put(name, "Example " + name);
    }
    variables.put("amount", "EUR 1.250,00");
    variables.put("baseUrl", "https://app.test");
    variables.put("primaryUrl", "https://app.test/primary");
    variables.put("secondaryUrl", "https://app.test/secondary");
    variables.put("count", 3);
    variables.put("truncatedCount", 1);
    variables.put("daysOverdue", 14);
    variables.put("daysRemaining", 30);
    variables.put("daysUntilExpiry", 30);
    variables.put("extensionNumber", 2);
    variables.put("renewalTermMonths", 12);
    variables.put("hasInstructions", false);
    variables.put("hasPartialPayment", false);
    variables.put("isFinal", false);
    variables.put("isOverdue", true);
    variables.put("tone", "FRIENDLY");
    // notification-digest iterates `items` and reads these three fields off each entry.
    Map<String, Object> digestItem =
        Map.of(
            "itemTitle", "Example item title",
            "itemSummary", "Example item summary",
            "itemLink", "https://app.test/item");
    variables.put("item", digestItem);
    variables.put("items", List.of(digestItem));
    variables.put("propertyNames", List.of("Example property"));
    return variables;
  }

  /** Every template except the shared fragment, which is not renderable on its own. */
  private static List<String> templateNames() throws IOException {
    try (Stream<Path> files = Files.list(templateDir())) {
      return files
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".html"))
          .filter(name -> !name.startsWith("_"))
          .map(name -> name.substring(0, name.length() - ".html".length()))
          .sorted()
          .toList();
    }
  }

  static Stream<Arguments> matrix() throws IOException {
    List<String> names = templateNames();
    // Pinned exactly: a floor lets a discovery regression drop a template silently. Adding a
    // template is a deliberate act, so updating this number with it is the right friction.
    assertThat(names).hasSize(24);

    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String name : names) {
        cases.add(Arguments.of(language, name));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("matrix")
  void rendersCleanly(String language, String templateName) {
    String html =
        engine().process(templateName, new Context(Locale.forLanguageTag(language), variables()));

    assertThat(html).as("[%s] %s rendered empty", language, templateName).isNotBlank();
    assertThat(html)
        .as("[%s] %s left an unresolved Thymeleaf message expression", language, templateName)
        .doesNotContain("#{");
    // useCodeAsDefaultMessage(true) means a missing key renders as the key ITSELF, never as
    // Thymeleaf's ??key?? marker — so a bare dotted key in the output is the real symptom.
    assertThat(html)
        .as("[%s] %s rendered a raw message key — the bundle is missing it", language, templateName)
        .doesNotContainPattern(MESSAGE_KEY)
        .doesNotContain("??");
    assertThat(html)
        .as("[%s] %s left a raw variable expression", language, templateName)
        .doesNotContain("${");
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("matrix")
  void resolvesANonEmptySubject(String language, String templateName) {
    String subject =
        new EmailSubjectResolver(messages())
            .resolve(templateName, variables(), Locale.forLanguageTag(language));

    assertThat(subject).as("[%s] %s has a blank subject", language, templateName).isNotBlank();
    assertThat(subject)
        .as("[%s] %s subject leaked a raw message key", language, templateName)
        .doesNotContainPattern(MESSAGE_KEY);
  }

  static Stream<Arguments> tenantReminderTones() {
    List<Arguments> cases = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      for (String tone : List.of("FRIENDLY", "FIRM", "FINAL")) {
        cases.add(Arguments.of(language, tone));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "[{0}] {1}")
  @MethodSource("tenantReminderTones")
  void resolvesADistinctSubjectPerTenantReminderTone(String language, String tone) {
    Map<String, Object> variables = variables();
    variables.put("tone", tone);

    String subject =
        new EmailSubjectResolver(messages())
            .resolve("payment-reminder-tenant", variables, Locale.forLanguageTag(language));

    // The tone keys are built dynamically (email.subject.payment-reminder-tenant.FIRM), so a
    // missing one resolves to the generic subject rather than failing loudly.
    assertThat(subject).as("[%s] tone %s has a blank subject", language, tone).isNotBlank();
    assertThat(subject)
        .as("[%s] tone %s subject leaked a raw message key", language, tone)
        .doesNotContainPattern(MESSAGE_KEY);
    assertThat(subject)
        .as("[%s] tone %s fell back to the default subject", language, tone)
        .isNotEqualTo(
            new EmailSubjectResolver(messages())
                .resolve("no-such-template", Map.of(), Locale.forLanguageTag(language)));
  }
}
