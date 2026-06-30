package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Size(min = 8) String password,
    Optional<String> invitationToken,
    Optional<String> registrationInvitationCode,
    @Pattern(regexp = "[a-z]{2}") String language) {
  public RegisterRequest {
    registrationInvitationCode =
        Objects.requireNonNullElse(registrationInvitationCode, Optional.empty());
    language = Objects.requireNonNullElse(language, "en");
  }
}
