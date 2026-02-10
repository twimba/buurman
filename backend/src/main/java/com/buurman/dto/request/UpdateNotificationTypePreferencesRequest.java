package com.buurman.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateNotificationTypePreferencesRequest(
    @Valid @NotNull List<Entry> preferences
) {
    public record Entry(
        @NotNull String notificationType,
        @NotNull Boolean emailEnabled,
        @NotNull Boolean smsEnabled
    ) {}
}
