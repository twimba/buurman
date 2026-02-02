package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record LinkTenantToPropertyRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        Instant movedInAt
) {}
