package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(@NotNull String newOwnerIdentifier) {}
