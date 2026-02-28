package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user account")
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Size(min = 8) String password,
    Optional<String> invitationToken,
    Optional<String> registrationInvitationCode) {
  public RegisterRequest {
    registrationInvitationCode =
        Objects.requireNonNullElse(registrationInvitationCode, Optional.empty());
  }
}
