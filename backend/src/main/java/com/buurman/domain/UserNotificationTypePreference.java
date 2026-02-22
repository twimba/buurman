package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationTypePreference {

  private @Nullable UUID id;
  private UUID userId;
  private NotificationType notificationType;
  @Builder.Default private boolean emailEnabled = true;
  @Builder.Default private boolean smsEnabled = false;
  private @Nullable Instant createdAt;
  private @Nullable Instant updatedAt;
}
