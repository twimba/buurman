package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class Notification {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private NotificationType notificationType;
  private String subject;
  private String body;
  private @Nullable String recipientEmail;
  private @Nullable String recipientPhone;
  private @Nullable UUID recipientUserId;
  private @Nullable UUID recipientTenantId;
  private NotificationChannel channel;
  private @Nullable String contentTemplate;
  private @Nullable Map<String, Object> contentVariables;
  private NotificationStatus status;
  private @Nullable String providerMessageId;
  private @Nullable String providerStatus;
  private @Nullable String providerError;
  private @Nullable Instant statusUpdatedAt;
  private int openCount;
  private int clickCount;
  private @Nullable Instant firstOpenedAt;
  private @Nullable Instant firstClickedAt;
  private @Nullable UUID resentFromId;
  private @Nullable String resendReason;
  private Instant createdAt;
  private UUID createdBy;
}
