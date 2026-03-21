package com.buurman.dto.request.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Generated
public record CreateBroadcastMessageRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String body,
    @NotBlank String severity,
    @NotBlank String scope,
    @NotNull Instant startAt,
    Optional<Instant> endAt,
    boolean showOnLogin,
    boolean showOnRegister,
    boolean showInApp,
    Optional<List<String>> targetTeamIdentifiers,
    Optional<List<String>> targetUserIdentifiers) {}
