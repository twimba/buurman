package com.buurman.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record UpdateNotificationTypePreferencesRequest(@Valid @NotNull List<Entry> preferences) {
  public record Entry(
      @NotNull String notificationType,
      @NotNull Boolean emailEnabled,
      @NotNull Boolean smsEnabled) {}
}
