package com.buurman.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferencesRequest(
    @Pattern(regexp = "light|dark|system") String theme,
    @Pattern(regexp = "[a-z]{2}") String language,
    String timezone,
    String dateFormat,
    String currencyFormat,
    Boolean emailNotifications,
    Boolean smsNotifications
) {}
