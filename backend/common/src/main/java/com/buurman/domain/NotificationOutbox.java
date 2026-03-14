package com.buurman.domain;

import java.time.Instant;
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
public class NotificationOutbox {

  private UUID id;
  private UUID notificationId;
  private NotificationChannel channel;
  private String payload;
  private OutboxStatus status;
  private int retryCount;
  private int maxRetries;
  private Instant nextRetryAt;
  @Builder.Default private Optional<String> lastError = Optional.empty();
  private Instant createdAt;
  @Builder.Default private Optional<Instant> processedAt = Optional.empty();
  @Builder.Default private Optional<NotificationType> notificationType = Optional.empty();
  @Builder.Default private Optional<UUID> recipientUserId = Optional.empty();
  @Builder.Default private Optional<String> recipientEmail = Optional.empty();
  @Builder.Default private Optional<UUID> teamId = Optional.empty();
  @Builder.Default private NotificationUrgency urgency = NotificationUrgency.NORMAL;
  @Builder.Default private Optional<UUID> consolidationGroupId = Optional.empty();
  private boolean consolidated;
}
