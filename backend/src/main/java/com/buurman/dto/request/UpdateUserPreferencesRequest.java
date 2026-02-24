package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferencesRequest(
    @Pattern(regexp = "light|dark|system") Optional<String> theme,
    @Pattern(regexp = "[a-z]{2}") Optional<String> language,
    Optional<String> timezone,
    Optional<String> dateFormat,
    Optional<String> currencyFormat,
    Optional<Boolean> emailNotifications,
    Optional<Boolean> smsNotifications) {}
