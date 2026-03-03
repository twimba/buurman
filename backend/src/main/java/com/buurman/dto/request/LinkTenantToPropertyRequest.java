package com.buurman.dto.request;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

import jakarta.validation.constraints.NotNull;

public record LinkTenantToPropertyRequest(
    @NotNull(message = "Property identifier is required") Ulid propertyIdentifier,
    Optional<Instant> movedInAt) {}
