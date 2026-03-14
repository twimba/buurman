package com.buurman.service.notification;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.config.models.NotificationConsolidationProperties;
import com.buurman.domain.DigestItem;
import com.buurman.domain.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DigestRenderer {

  private final NotificationConsolidationProperties consolidationProperties;

  /**
   * Build template variables for a digest email.
   *
   * @param notificationType the shared notification type
   * @param items pre-built digest items (already mapped from contentVariables)
   * @param recipientName first name of the recipient
   * @param baseUrl application base URL
   * @return map of template variables for notification-digest.html
   */
  public Map<String, Object> buildEmailDigestVariables(
      NotificationType notificationType,
      List<DigestItem> items,
      String recipientName,
      String baseUrl) {
    int maxItems = consolidationProperties.email().maxItemsPerDigest();
    int totalCount = items.size();

    List<DigestItem> displayItems;
    int truncatedCount;
    if (totalCount > maxItems) {
      displayItems = items.subList(0, maxItems);
      truncatedCount = totalCount - maxItems;
    } else {
      displayItems = items;
      truncatedCount = 0;
    }

    String typeName = notificationType.getDisplayName();
    String introText = buildIntroText(notificationType, totalCount);
    String ctaText = buildCtaText(notificationType);
    String ctaUrl = buildCtaUrl(notificationType, baseUrl);

    return Map.of(
        "count", totalCount,
        "typeName", typeName,
        "userName", recipientName,
        "introText", introText,
        "items", displayItems,
        "truncatedCount", truncatedCount,
        "ctaText", ctaText,
        "ctaUrl", ctaUrl);
  }

  /** Build subject line for a digest email. */
  public String buildEmailDigestSubject(NotificationType notificationType, List<DigestItem> items) {
    int count = items.size();
    String typeName = notificationType.getDisplayName();

    return extractCommonProperty(items)
        .map(property -> count + " " + typeName + " for " + property)
        .orElse(count + " " + typeName);
  }

  /** Build SMS summary text, fitting within 160 chars (one GSM-7 segment). */
  public String buildSmsSummary(
      NotificationType notificationType, List<DigestItem> items, String baseUrl) {
    int count = items.size();
    String typeName = notificationType.getDisplayName().toLowerCase(Locale.ROOT);

    String base = "Buurman: You have " + count + " " + typeName + ". ";
    String link = "View: " + baseUrl;

    if (!items.isEmpty()) {
      String detail = items.getFirst().itemSummary();
      String withDetail = base + detail + ". " + link;
      if (withDetail.length() <= 160) {
        return withDetail;
      }
    }

    return base + link;
  }

  /** Build a DigestItem from a notification's contentVariables based on its type. */
  public DigestItem buildDigestItem(
      NotificationType notificationType, Map<String, Object> contentVariables, String baseUrl) {
    return switch (notificationType) {
      case PAYMENT_REMINDER ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              getVar(contentVariables, "amount", "")
                  + " — Due "
                  + getVar(contentVariables, "dueDate", ""),
              baseUrl + "/payments",
              contentVariables);
      case CONTRACT_EXPIRY ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              "Expires "
                  + getVar(contentVariables, "expiryDate", "")
                  + " ("
                  + getVar(contentVariables, "daysUntilExpiry", "?")
                  + " days)",
              baseUrl + "/contracts",
              contentVariables);
      case EXPENSE_CREATED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              getVar(contentVariables, "amount", "")
                  + " — "
                  + getVar(contentVariables, "category", "Expense"),
              baseUrl + "/expenses",
              contentVariables);
      case PAYMENT_PAID, PAYMENT_RECEIVAL ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              getVar(contentVariables, "amount", getVar(contentVariables, "receivalAmount", "")),
              baseUrl + "/payments",
              contentVariables);
      case CONTRACT_RENT_ADJUSTED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              getVar(contentVariables, "oldRent", "?")
                  + " → "
                  + getVar(contentVariables, "newRent", "?")
                  + " (effective "
                  + getVar(contentVariables, "effectiveDate", "")
                  + ")",
              baseUrl + "/contracts",
              contentVariables);
      case INVITATION_ACCEPTED ->
          new DigestItem(
              getVar(contentVariables, "memberName", "Team member"),
              "Joined " + getVar(contentVariables, "teamName", "your team"),
              baseUrl + "/settings/team",
              contentVariables);
      case PROPERTY_CREATED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              "New property created",
              baseUrl + "/properties",
              contentVariables);
      case CONTRACT_CREATED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              "New contract with " + getVar(contentVariables, "tenantName", "tenant"),
              baseUrl + "/contracts",
              contentVariables);
      case CONTRACT_STATUS_CHANGED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              "Status changed to " + getVar(contentVariables, "newStatus", "updated"),
              baseUrl + "/contracts",
              contentVariables);
      case CONTRACT_REOPENED ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Property"),
              "Contract reopened for editing",
              baseUrl + "/contracts",
              contentVariables);
      default ->
          new DigestItem(
              getVar(contentVariables, "propertyName", "Notification"),
              notificationType.getDisplayName(),
              baseUrl,
              contentVariables);
    };
  }

  private String buildIntroText(NotificationType type, int count) {
    return switch (type) {
      case PAYMENT_REMINDER -> "You have " + count + " upcoming payment reminders.";
      case CONTRACT_EXPIRY -> "You have " + count + " contracts expiring soon.";
      case EXPENSE_CREATED -> count + " new expenses have been recorded.";
      case PAYMENT_PAID -> count + " payments have been marked as paid.";
      case PAYMENT_RECEIVAL -> count + " payment receivals have been registered.";
      case CONTRACT_RENT_ADJUSTED -> count + " contract rents have been adjusted.";
      case INVITATION_ACCEPTED -> count + " team members have accepted their invitations.";
      case PROPERTY_CREATED -> count + " new properties have been created.";
      case CONTRACT_CREATED -> count + " new contracts have been created.";
      case CONTRACT_STATUS_CHANGED -> count + " contract statuses have changed.";
      case CONTRACT_REOPENED -> count + " contracts have been reopened.";
      default -> "You have " + count + " new notifications.";
    };
  }

  private String buildCtaText(NotificationType type) {
    return switch (type) {
      case PAYMENT_REMINDER, PAYMENT_PAID, PAYMENT_RECEIVAL -> "View All Payments";
      case CONTRACT_EXPIRY,
          CONTRACT_CREATED,
          CONTRACT_STATUS_CHANGED,
          CONTRACT_REOPENED,
          CONTRACT_RENT_ADJUSTED ->
          "View All Contracts";
      case EXPENSE_CREATED -> "View All Expenses";
      case PROPERTY_CREATED -> "View All Properties";
      case INVITATION_ACCEPTED -> "View Team Settings";
      default -> "View All";
    };
  }

  private String buildCtaUrl(NotificationType type, String baseUrl) {
    return switch (type) {
      case PAYMENT_REMINDER, PAYMENT_PAID, PAYMENT_RECEIVAL -> baseUrl + "/payments";
      case CONTRACT_EXPIRY,
          CONTRACT_CREATED,
          CONTRACT_STATUS_CHANGED,
          CONTRACT_REOPENED,
          CONTRACT_RENT_ADJUSTED ->
          baseUrl + "/contracts";
      case EXPENSE_CREATED -> baseUrl + "/expenses";
      case PROPERTY_CREATED -> baseUrl + "/properties";
      case INVITATION_ACCEPTED -> baseUrl + "/settings/team";
      default -> baseUrl;
    };
  }

  private Optional<String> extractCommonProperty(List<DigestItem> items) {
    if (items.isEmpty()) {
      return Optional.empty();
    }
    String first = items.getFirst().contentVariables().getOrDefault("propertyName", "").toString();
    if (first.isEmpty()) {
      return Optional.empty();
    }
    boolean allSame =
        items.stream()
            .allMatch(
                item ->
                    first.equals(
                        item.contentVariables().getOrDefault("propertyName", "").toString()));
    return allSame ? Optional.of(first) : Optional.empty();
  }

  private String getVar(@Nullable Map<String, Object> variables, String key, String defaultValue) {
    if (variables == null) {
      return defaultValue;
    }
    Object val = variables.get(key);
    if (val == null) {
      return defaultValue;
    }
    if (val instanceof Optional<?> opt) {
      return opt.map(Object::toString).orElse(defaultValue);
    }
    return val.toString();
  }
}
