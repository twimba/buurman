package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferencesRequest(
    @Nullable @Pattern(regexp = "light|dark|system") String theme,
    @Nullable @Pattern(regexp = "[a-z]{2}") String language,
    @Nullable String timezone,
    @Nullable String dateFormat,
    @Nullable String currencyFormat,
    @Nullable Boolean emailNotifications,
    @Nullable Boolean smsNotifications) {}
