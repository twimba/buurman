package com.buurman.dto.request;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record SwitchTeamRequest(@NotNull TeamIdentifier teamIdentifier) {}
