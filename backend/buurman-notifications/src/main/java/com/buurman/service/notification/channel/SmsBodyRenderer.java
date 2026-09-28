package com.buurman.service.notification.channel;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.buurman.util.SmsSegment;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders an SMS body from the localized {@code sms-bodies} bundle and guarantees it fits one
 * segment. Both senders delegate here, so the copy lives in one place and is actually localized —
 * they previously carried byte-identical English {@code switch} blocks and discarded the locale.
 */
@Component
@Slf4j
public class SmsBodyRenderer {

  private static final String KEY_PREFIX = "sms.body.";
  private static final String DEFAULT_KEY = KEY_PREFIX + "default";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

  private final MessageSource notificationMessageSource;

  public SmsBodyRenderer(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource) {
    this.notificationMessageSource = notificationMessageSource;
  }

  public String render(
      String templateName, @Nullable Map<String, Object> variables, Locale locale) {
    String template = resolveTemplate(templateName, locale);
    String body = interpolate(template, variables);
    if (hasUnfilledPlaceholder(template, variables)) {
      // A garbled message with a raw {placeholder} in it is worse than a correct generic one.
      log.warn("SMS template {} left a placeholder unfilled; using the generic body", templateName);
      body = genericBody(locale);
    }
    return SmsSegment.fitToOneSegment(body, longestValueIn(body, variables));
  }

  private String resolveTemplate(String templateName, Locale locale) {
    String key = KEY_PREFIX + templateName;
    // The notification MessageSource sets useCodeAsDefaultMessage(true), so an unknown key comes
    // back as the key itself rather than null.
    String resolved =
        Optional.ofNullable(notificationMessageSource.getMessage(key, null, key, locale))
            .orElse(key);
    return key.equals(resolved) ? genericBody(locale) : resolved;
  }

  private String genericBody(Locale locale) {
    return Optional.ofNullable(notificationMessageSource.getMessage(DEFAULT_KEY, null, "", locale))
        .orElse("");
  }

  private String interpolate(String template, @Nullable Map<String, Object> variables) {
    if (variables == null) {
      return template;
    }
    String result = template;
    for (Map.Entry<String, Object> variable : variables.entrySet()) {
      result = result.replace("{" + variable.getKey() + "}", String.valueOf(variable.getValue()));
    }
    return result;
  }

  /**
   * Checks the TEMPLATE, not the rendered body: a landlord may legitimately name a unit {@code Unit
   * {A1}}, and matching braces after interpolation would mistake that tenant's own data for an
   * unfilled token and replace their whole message with the generic one.
   */
  private boolean hasUnfilledPlaceholder(String template, @Nullable Map<String, Object> variables) {
    Set<String> supplied = variables == null ? Set.of() : variables.keySet();
    Matcher placeholders = PLACEHOLDER.matcher(template);
    while (placeholders.find()) {
      if (!supplied.contains(placeholders.group(1))) {
        return true;
      }
    }
    return false;
  }

  /**
   * The longest value that actually appears in the body. Callers pass a superset of variables —
   * {@code payment-reminder} is handed a {@code baseUrl} its SMS body never uses — and shortening a
   * value that is not present would leave the body unchanged and force the sentence itself to be
   * truncated instead.
   */
  private String longestValueIn(String body, @Nullable Map<String, Object> variables) {
    if (variables == null) {
      return "";
    }
    return variables.values().stream()
        .map(String::valueOf)
        .filter(value -> !value.isBlank())
        .filter(body::contains)
        .max(Comparator.comparingInt(String::length))
        .orElse("");
  }
}
