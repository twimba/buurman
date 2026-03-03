package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record PhotoResponse(
    Ulid identifier,
    String entityType,
    Ulid entityIdentifier,
    String fileKey,
    String fileName,
    Long fileSize,
    String mimeType,
    Optional<String> title,
    Optional<String> notes,
    Boolean isMainPhoto,
    Instant uploadedAt,
    Optional<String> downloadUrl,
    Optional<String> thumbnailUrl) {}
