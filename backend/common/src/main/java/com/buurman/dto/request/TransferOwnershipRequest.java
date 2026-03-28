package com.buurman.dto.request;

import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record TransferOwnershipRequest(@NotNull UserIdentifier newOwnerIdentifier) {}
