package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Size(min = 8) String password,
    @Nullable String invitationToken,
    @Nullable String registrationInvitationCode) {}
