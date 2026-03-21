package com.buurman.dto.request;

import com.buurman.domain.identifier.UserIdentifier;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record TransferOwnershipRequest(@NotNull UserIdentifier newOwnerIdentifier) {}
