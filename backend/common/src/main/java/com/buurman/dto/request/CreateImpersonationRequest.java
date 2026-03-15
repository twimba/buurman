package com.buurman.dto.request;

import java.time.Duration;
import java.util.Optional;

import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateImpersonationRequest(
    @NotNull(message = "User identifier is required") UserIdentifier userIdentifier,
    @NotNull(message = "Team identifier is required") TeamIdentifier teamIdentifier,
    @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason,
    @NotBlank(message = "Password is required") String password,
    Optional<ImpersonationMode> mode,
    Optional<Duration> timeout) {}
