package com.buurman.dto.request;

import com.buurman.domain.identifier.UserIdentifier;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(@NotNull UserIdentifier newOwnerIdentifier) {}
