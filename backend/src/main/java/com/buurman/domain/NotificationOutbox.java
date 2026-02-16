package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

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
  private String lastError;
  private Instant createdAt;
  private Instant processedAt;
}
