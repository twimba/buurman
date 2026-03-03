package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record DocumentResponse(
    Ulid identifier,
    String entityType,
    Ulid entityIdentifier,
    String fileKey,
    String fileName,
    Optional<Long> fileSize,
    Optional<String> mimeType,
    Optional<String> title,
    Optional<String> notes,
    Instant uploadedAt,
    Optional<String> downloadUrl) {}
