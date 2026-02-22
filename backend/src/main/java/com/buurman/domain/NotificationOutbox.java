package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class NotificationOutbox {

  private UUID id;
  private UUID notificationId;
  private NotificationChannel channel;
  private String payload;
  private OutboxStatus status;
  private int retryCount;
  private int maxRetries;
  private Instant nextRetryAt;
  private @Nullable String lastError;
  private Instant createdAt;
  private @Nullable Instant processedAt;
}
