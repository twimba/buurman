package com.buurman.dto.request;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.identifier.PropertyIdentifier;

import jakarta.validation.constraints.NotNull;

public record LinkTenantToPropertyRequest(
    @NotNull(message = "Property identifier is required") PropertyIdentifier propertyIdentifier,
    Optional<Instant> movedInAt) {}
