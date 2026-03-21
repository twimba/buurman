package com.buurman.dto.request;

import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record TransferOwnershipRequest(@NotNull UserIdentifier newOwnerIdentifier) {}
