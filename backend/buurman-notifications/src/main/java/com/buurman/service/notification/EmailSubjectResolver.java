package com.buurman.service.notification;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class EmailSubjectResolver {

  private final MessageSource notificationMessageSource;

  public EmailSubjectResolver(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource) {
    this.notificationMessageSource = notificationMessageSource;
  }

  private static final String FALLBACK_SUBJECT = "Notification from Buurman";

  public String resolve(String templateName, Map<String, Object> variables, Locale locale) {
    String key = "email.subject." + templateName;
    Object[] args = extractArgs(templateName, variables);
    String defaultSubject =
        Optional.ofNullable(
                notificationMessageSource.getMessage("email.subject.default", null, null, locale))
            .orElse(FALLBACK_SUBJECT);
    // A tenant reminder reads very differently by tone, so each tone has its own subject line.
    return toneVariantKey(templateName, variables)
        .flatMap(
            k -> Optional.ofNullable(notificationMessageSource.getMessage(k, args, null, locale)))
        .or(
            () ->
                Optional.ofNullable(notificationMessageSource.getMessage(key, args, null, locale)))
        .orElse(defaultSubject);
  }

  private Optional<String> toneVariantKey(String templateName, Map<String, Object> variables) {
    if (!"payment-reminder-tenant".equals(templateName)) {
      return Optional.empty();
    }
    String tone = getVar(variables, "tone", "");
    return tone.isBlank()
        ? Optional.empty()
        : Optional.of("email.subject." + templateName + "." + tone);
  }

  private Object[] extractArgs(String templateName, Map<String, Object> variables) {
    return switch (templateName) {
      case "team-invitation" -> new Object[] {getVar(variables, "teamName", "a team")};
      case "invitation-accepted" ->
          new Object[] {
            getVar(variables, "memberName", "Someone"), getVar(variables, "teamName", "your team")
          };
      case "payment-reminder",
          "payment-reminder-tenant",
          "contract-expiry",
          "property-created",
          "contract-created",
          "contract-reopened",
          "payment-paid",
          "payment-receival",
          "expense-created" ->
          new Object[] {getVar(variables, "propertyName", "your property")};
      case "contract-status-changed" -> new Object[] {getVar(variables, "newStatus", "updated")};
      case "notification-digest" ->
          new Object[] {
            getVar(variables, "count", ""), getVar(variables, "typeName", "Notifications")
          };
      default -> new Object[] {};
    };
  }

  private String getVar(Map<String, Object> variables, String key, String defaultValue) {
    if (variables == null) {
      return defaultValue;
    }
    return Optional.ofNullable(variables.get(key)).map(Object::toString).orElse(defaultValue);
  }
}
