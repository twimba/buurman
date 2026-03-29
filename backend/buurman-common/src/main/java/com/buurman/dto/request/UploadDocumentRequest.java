package com.buurman.dto.request;

import java.util.Optional;
import java.util.UUID;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record UploadDocumentRequest(
    @NotBlank(message = "Entity type is required") String entityType,
    @NotNull(message = "Entity ID is required") UUID entityId,
    Optional<String> title,
    Optional<String> notes) {}
