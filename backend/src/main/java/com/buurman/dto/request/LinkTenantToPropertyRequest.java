package com.buurman.dto.request;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;

public record LinkTenantToPropertyRequest(
    @NotNull(message = "Property identifier is required") String propertyIdentifier,
    @Nullable Instant movedInAt) {}
