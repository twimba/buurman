package com.buurman.dto.request;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record SwitchTeamRequest(@NotNull TeamIdentifier teamIdentifier) {}
