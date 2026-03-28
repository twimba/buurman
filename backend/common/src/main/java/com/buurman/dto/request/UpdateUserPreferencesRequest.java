package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Pattern;

@SkipTestCoverage
public record UpdateUserPreferencesRequest(
    Optional<@Pattern(regexp = "light|dark|system") String> theme,
    Optional<@Pattern(regexp = "[a-z]{2}") String> language,
    Optional<String> timezone,
    Optional<String> dateFormat,
    Optional<String> currencyFormat,
    Optional<Boolean> emailNotifications,
    Optional<Boolean> smsNotifications) {}
