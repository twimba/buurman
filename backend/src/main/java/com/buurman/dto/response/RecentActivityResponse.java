package com.buurman.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecentActivityResponse(
        UUID id,
        String entityType,
        UUID entityId,
        String entityName,
        String action,
        String userName,
        Instant timestamp,
        String description,
        Map<String, Object> changedFields,
        Map<String, Object> oldValues,
        Map<String, Object> newValues
) {
    public RecentActivityResponse(
            UUID id,
            String entityType,
            UUID entityId,
            String entityName,
            String action,
            String userName,
            Instant timestamp,
            String description
    ) {
        this(id, entityType,entityId,entityName,action,userName,timestamp,description, Map.of(),Map.of(),Map.of());
    }
}
