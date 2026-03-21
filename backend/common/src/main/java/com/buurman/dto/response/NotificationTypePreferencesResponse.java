package com.buurman.dto.response;

import java.util.List;
import com.buurman.util.Generated;

@Generated
public record NotificationTypePreferencesResponse(
    boolean globalEmailEnabled,
    boolean globalSmsEnabled,
    boolean smsAvailable,
    boolean emailAvailable,
    List<Entry> preferences) {
  public record Entry(
      String notificationType, String displayName, boolean emailEnabled, boolean smsEnabled) {}
}
