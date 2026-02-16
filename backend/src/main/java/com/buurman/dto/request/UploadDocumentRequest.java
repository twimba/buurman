package com.buurman.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadDocumentRequest(
    @NotBlank(message = "Entity type is required") String entityType,
    @NotNull(message = "Entity ID is required") UUID entityId,
    String title,
    String notes) {}
