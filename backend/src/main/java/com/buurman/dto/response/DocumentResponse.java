package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID teamId,
        String entityType,
        UUID entityId,
        String fileKey,
        String fileName,
        Long fileSize,
        String mimeType,
        String title,
        String notes,
        String category,
        Boolean isMainPhoto,
        UUID uploadedBy,
        Instant uploadedAt,
        String downloadUrl
) {}
