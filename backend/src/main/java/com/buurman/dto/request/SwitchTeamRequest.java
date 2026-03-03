package com.buurman.dto.request;

import com.buurman.domain.Ulid;

import jakarta.validation.constraints.NotNull;

public record SwitchTeamRequest(@NotNull Ulid teamIdentifier) {}
