package com.buurman.dto.request.backoffice;

import java.time.Instant;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBroadcastMessageRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String body,
    @NotBlank String severity,
    @NotNull Instant startAt,
    Optional<Instant> endAt,
    boolean showOnLogin,
    boolean showOnRegister,
    boolean showInApp) {}
