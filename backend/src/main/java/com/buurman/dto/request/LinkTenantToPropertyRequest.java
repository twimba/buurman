package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record LinkTenantToPropertyRequest(
        @NotNull(message = "Property identifier is required")
        String propertyIdentifier,

        Instant movedInAt
) {}
