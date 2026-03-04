package com.buurman.dto.request;

import com.buurman.domain.identifier.TeamIdentifier;

import jakarta.validation.constraints.NotNull;

public record SwitchTeamRequest(@NotNull TeamIdentifier teamIdentifier) {}
