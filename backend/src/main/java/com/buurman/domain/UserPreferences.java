package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;

@SuppressWarnings("NullAway.Init")
@Data
public class UserPreferences {

  private UUID id;
  private UUID userId;
  private String theme = "system";
  private String language = "en";
  private String timezone = "UTC";
  private String dateFormat = "DD/MM/YYYY";
  private @Nullable String currencyFormat;
  private boolean emailNotifications = true;
  private boolean inAppNotifications = true;
  private boolean smsNotifications = false;
  private Instant createdAt;
  private Instant updatedAt;
}
