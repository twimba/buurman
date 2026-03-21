package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record UserPreferencesResponse(
    Optional<String> theme,
    Optional<String> language,
    Optional<String> timezone,
    Optional<String> dateFormat,
    Optional<String> currencyFormat,
    boolean emailNotifications,
    boolean smsNotifications) {}
