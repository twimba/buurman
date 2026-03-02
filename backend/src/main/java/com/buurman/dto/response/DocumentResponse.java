package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record DocumentResponse(
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
