package com.buurman.dto.response;

import java.time.Instant;

public record DocumentResponse(
    String identifier,
    String entityType,
    String entityIdentifier,
    String fileKey,
    String fileName,
    Long fileSize,
    String mimeType,
    String title,
    String notes,
    Instant uploadedAt,
    String downloadUrl) {}
