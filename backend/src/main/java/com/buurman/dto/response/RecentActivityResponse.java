package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RecentActivityResponse(
        UUID id,
        String entityType,
        UUID entityId,
        String entityName,
        String action,
        String userName,
        Instant timestamp,
        String description
) {}
