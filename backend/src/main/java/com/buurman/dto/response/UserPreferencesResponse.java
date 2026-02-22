package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record UserPreferencesResponse(
    @Nullable String theme,
    @Nullable String language,
    @Nullable String timezone,
    @Nullable String dateFormat,
    @Nullable String currencyFormat,
    boolean emailNotifications,
    boolean smsNotifications) {}
