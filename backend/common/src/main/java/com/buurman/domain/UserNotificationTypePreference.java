package com.buurman.domain;

import java.time.Instant;
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
public class UserNotificationTypePreference {

  private UUID id;
  private UUID userId;
  private NotificationType notificationType;
  @Builder.Default private boolean emailEnabled = true;
  @Builder.Default private boolean smsEnabled = false;
  private Instant createdAt;
  private Instant updatedAt;
}
