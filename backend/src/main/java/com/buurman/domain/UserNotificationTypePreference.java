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
public class UserNotificationTypePreference {

  @Builder.Default private Optional<UUID> id = Optional.empty();
  private UUID userId;
  private NotificationType notificationType;
  @Builder.Default private boolean emailEnabled = true;
  @Builder.Default private boolean smsEnabled = false;
  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
}
