package com.buurman.service.notification;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record NotificationSendRequest(
    Optional<UUID> notificationId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    Optional<String> subject,
    String body,
    Optional<String> fromEmail,
    Optional<String> fromName) {
  public NotificationSendRequest {
    notificationId = Objects.requireNonNullElse(notificationId, Optional.empty());
    recipientEmail = Objects.requireNonNullElse(recipientEmail, Optional.empty());
    recipientPhone = Objects.requireNonNullElse(recipientPhone, Optional.empty());
    subject = Objects.requireNonNullElse(subject, Optional.empty());
    fromEmail = Objects.requireNonNullElse(fromEmail, Optional.empty());
    fromName = Objects.requireNonNullElse(fromName, Optional.empty());
  }
}
