package com.buurman.service.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record NotificationSendRequest(
    Optional<UUID> notificationId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    Optional<String> subject,
    String body,
    Optional<String> fromEmail,
    Optional<String> fromName,
    Optional<List<EmailAttachment>> attachments) {

  public NotificationSendRequest(
      Optional<UUID> notificationId,
      Optional<String> recipientEmail,
      Optional<String> recipientPhone,
      Optional<String> subject,
      String body,
      Optional<String> fromEmail,
      Optional<String> fromName) {
    this(
        notificationId,
        recipientEmail,
        recipientPhone,
        subject,
        body,
        fromEmail,
        fromName,
        Optional.empty());
  }
}
