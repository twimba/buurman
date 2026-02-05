package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetDefaultTeamRequest(
    @NotNull String teamIdentifier
) {}
