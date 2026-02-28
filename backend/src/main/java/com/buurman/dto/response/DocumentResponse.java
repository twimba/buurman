package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uploaded document metadata with download URL")
public record DocumentResponse(
    @Schema(description = "Unique document identifier", example = "doc_01HZQX7V8B3K5M2N4P6R9T0W")
        String identifier,
    String entityType,
    String entityIdentifier,
    String fileKey,
    String fileName,
    Optional<Long> fileSize,
    Optional<String> mimeType,
    Optional<String> title,
    Optional<String> notes,
    Instant uploadedAt,
    Optional<String> downloadUrl) {}
