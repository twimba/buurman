package com.buurman.dto.response;

public record UserPreferencesResponse(
    String theme,
    String language,
    String timezone,
    String dateFormat,
    String currencyFormat,
    boolean emailNotifications,
    boolean smsNotifications
) {}
