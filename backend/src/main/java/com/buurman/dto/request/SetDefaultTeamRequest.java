package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetDefaultTeamRequest(
    @NotNull UUID teamId
) {}
