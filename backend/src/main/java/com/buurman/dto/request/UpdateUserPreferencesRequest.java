package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferencesRequest(
    @Pattern(regexp = "light|dark|system") Optional<String> theme,
    @Pattern(regexp = "[a-z]{2}") Optional<String> language,
    Optional<String> timezone,
    Optional<String> dateFormat,
    Optional<String> currencyFormat,
    Optional<Boolean> emailNotifications,
    Optional<Boolean> smsNotifications) {
  public UpdateUserPreferencesRequest {
    theme = Objects.requireNonNullElse(theme, Optional.empty());
    language = Objects.requireNonNullElse(language, Optional.empty());
    timezone = Objects.requireNonNullElse(timezone, Optional.empty());
    dateFormat = Objects.requireNonNullElse(dateFormat, Optional.empty());
    currencyFormat = Objects.requireNonNullElse(currencyFormat, Optional.empty());
    emailNotifications = Objects.requireNonNullElse(emailNotifications, Optional.empty());
    smsNotifications = Objects.requireNonNullElse(smsNotifications, Optional.empty());
  }
}
