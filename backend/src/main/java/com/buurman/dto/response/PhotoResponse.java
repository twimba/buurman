package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record PhotoResponse(
    String identifier,
    String entityType,
    String entityIdentifier,
    String fileKey,
    String fileName,
    @Nullable Long fileSize,
    @Nullable String mimeType,
    @Nullable String title,
    @Nullable String notes,
    Boolean isMainPhoto,
    Instant uploadedAt,
    @Nullable String downloadUrl,
    @Nullable String thumbnailUrl) {}
