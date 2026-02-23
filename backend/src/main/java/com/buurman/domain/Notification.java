package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  private UUID id;
  private String identifier;
  @Builder.Default private Optional<UUID> teamId = Optional.empty();
  private NotificationType notificationType;
  @Builder.Default private Optional<String> subject = Optional.empty();
  private String body;
  @Builder.Default private Optional<String> recipientEmail = Optional.empty();
  @Builder.Default private Optional<String> recipientPhone = Optional.empty();
  @Builder.Default private Optional<UUID> recipientUserId = Optional.empty();
  @Builder.Default private Optional<UUID> recipientTenantId = Optional.empty();
  private NotificationChannel channel;
  @Builder.Default private Optional<String> contentTemplate = Optional.empty();
  @Builder.Default private Optional<Map<String, Object>> contentVariables = Optional.empty();
  private NotificationStatus status;
  @Builder.Default private Optional<String> providerMessageId = Optional.empty();
  @Builder.Default private Optional<String> providerStatus = Optional.empty();
  @Builder.Default private Optional<String> providerError = Optional.empty();
  @Builder.Default private Optional<Instant> statusUpdatedAt = Optional.empty();
  private int openCount;
  private int clickCount;
  @Builder.Default private Optional<Instant> firstOpenedAt = Optional.empty();
  @Builder.Default private Optional<Instant> firstClickedAt = Optional.empty();
  @Builder.Default private Optional<UUID> resentFromId = Optional.empty();
  @Builder.Default private Optional<String> resendReason = Optional.empty();
  private Instant createdAt;
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
}
