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
public class UserPreferences {

  private UUID id;
  private UUID userId;
  @Builder.Default private String theme = "system";
  @Builder.Default private String language = "en";
  @Builder.Default private String timezone = "UTC";
  @Builder.Default private String dateFormat = "DD/MM/YYYY";
  private @Nullable String currencyFormat;
  @Builder.Default private boolean emailNotifications = true;
  @Builder.Default private boolean inAppNotifications = true;
  @Builder.Default private boolean smsNotifications = false;
  private Instant createdAt;
  private Instant updatedAt;
}
