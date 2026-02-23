package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record PhotoResponse(
    String identifier,
    String entityType,
    String entityIdentifier,
    String fileKey,
    String fileName,
    Optional<Long> fileSize,
    Optional<String> mimeType,
    Optional<String> title,
    Optional<String> notes,
    Boolean isMainPhoto,
    Instant uploadedAt,
    Optional<String> downloadUrl,
    Optional<String> thumbnailUrl) {}
