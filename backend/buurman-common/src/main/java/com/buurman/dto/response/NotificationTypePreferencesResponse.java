package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record NotificationTypePreferencesResponse(
    boolean globalEmailEnabled,
    boolean globalSmsEnabled,
    boolean smsAvailable,
    boolean smsFeatureEnabled,
    boolean emailAvailable,
    boolean phoneVerified,
    List<Entry> preferences) {
  public record Entry(
      String notificationType, String displayName, boolean emailEnabled, boolean smsEnabled) {}
}
