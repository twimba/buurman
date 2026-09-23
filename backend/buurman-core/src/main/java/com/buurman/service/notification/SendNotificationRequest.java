package com.buurman.service.notification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;

import lombok.Builder;

@Builder
public record SendNotificationRequest(
    Optional<UUID> teamId,
    NotificationType notificationType,
    Optional<UUID> recipientUserId,
    Optional<UUID> recipientContactId,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    /** Explicit locale for rendering (BCP 47 tag). Overrides user/team preference resolution. */
    Optional<String> languageTag,
    String templateName,
    Map<String, Object> templateVariables,
    NotificationUrgency urgency,
    UUID createdBy) {

  /** Customize the Lombok-generated builder to provide defaults for Optional and urgency fields. */
  @SuppressWarnings("NullAway.Init")
  public static class SendNotificationRequestBuilder {
    private Optional<UUID> teamId = Optional.empty();
    private Optional<UUID> recipientUserId = Optional.empty();
    private Optional<UUID> recipientContactId = Optional.empty();
    private Optional<String> recipientEmail = Optional.empty();
    private Optional<String> recipientPhone = Optional.empty();
    private Optional<String> languageTag = Optional.empty();
    private NotificationUrgency urgency = NotificationUrgency.NORMAL;
  }
}
