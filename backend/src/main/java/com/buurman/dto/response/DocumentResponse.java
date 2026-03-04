package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;

public record DocumentResponse(
    Sid identifier,
    String entityType,
    Sid entityIdentifier,
    String fileKey,
    String fileName,
    Optional<Long> fileSize,
    Optional<String> mimeType,
    Optional<String> title,
    Optional<String> notes,
    Instant uploadedAt,
    Optional<String> downloadUrl) {}
