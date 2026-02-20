package com.buurman.dto.response;

import java.util.List;

public record NotificationTypePreferencesResponse(
    boolean globalEmailEnabled,
    boolean globalSmsEnabled,
    boolean smsAvailable,
    List<Entry> preferences) {
  public record Entry(
      String notificationType, String displayName, boolean emailEnabled, boolean smsEnabled) {}
}
