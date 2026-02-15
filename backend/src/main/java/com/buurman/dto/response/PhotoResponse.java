package com.buurman.dto.response;

import java.time.Instant;

public record PhotoResponse(
        String identifier,
        String entityType,
        String entityIdentifier,
        String fileKey,
        String fileName,
        Long fileSize,
        String mimeType,
        String title,
        String notes,
        Boolean isMainPhoto,
        Instant uploadedAt,
        String downloadUrl,
        String thumbnailUrl
) {}
