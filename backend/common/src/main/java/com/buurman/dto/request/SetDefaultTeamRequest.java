package com.buurman.dto.request;

import com.buurman.domain.identifier.TeamIdentifier;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record SetDefaultTeamRequest(@NotNull TeamIdentifier teamIdentifier) {}
