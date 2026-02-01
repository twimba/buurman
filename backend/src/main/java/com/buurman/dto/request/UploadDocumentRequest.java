package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadDocumentRequest(
        @NotBlank(message = "Entity type is required")
        String entityType,

        @NotNull(message = "Entity ID is required")
        UUID entityId,

        String title,

        String notes
) {}
