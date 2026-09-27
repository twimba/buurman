package com.buurman.service.notification.channel;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-zA-Z0-9_]+}");

  private final MessageSource notificationMessageSource;

  public SmsBodyRenderer(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource) {
    this.notificationMessageSource = notificationMessageSource;
  }

  public String render(
      String templateName, @Nullable Map<String, Object> variables, Locale locale) {
    String body = interpolate(resolveTemplate(templateName, locale), variables);
    if (PLACEHOLDER.matcher(body).find()) {
      // A garbled message with a raw {placeholder} in it is worse than a correct generic one.
      log.warn("SMS template {} left a placeholder unfilled; using the generic body", templateName);
      body = genericBody(locale);
    }
    return SmsSegment.fitToOneSegment(body, longestValue(variables));
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

  private String longestValue(@Nullable Map<String, Object> variables) {
    if (variables == null) {
      return "";
    }
    return variables.values().stream()
        .map(String::valueOf)
        .filter(value -> !value.isBlank())
        .max(Comparator.comparingInt(String::length))
        .orElse("");
  }
}
