package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class UserNotificationTypePreference {

  private UUID id;
  private UUID userId;
  private NotificationType notificationType;
  private boolean emailEnabled = true;
  private boolean smsEnabled = false;
  private Instant createdAt;
  private Instant updatedAt;
}
