package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;

@SuppressWarnings("NullAway.Init")
@Data
public class UserNotificationTypePreference {

  private @Nullable UUID id;
  private UUID userId;
  private NotificationType notificationType;
  private boolean emailEnabled = true;
  private boolean smsEnabled = false;
  private @Nullable Instant createdAt;
  private @Nullable Instant updatedAt;
}
