package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

public record SwitchTeamRequest(@NotNull String teamIdentifier) {}
