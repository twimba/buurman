package com.buurman.dto.response;

import java.time.Instant;
import java.util.Map;

public record RecentActivityResponse(
        String entityType,
        String entityIdentifier,
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
            String entityType,
            String entityIdentifier,
            String entityName,
            String action,
            String userName,
            Instant timestamp,
            String description
    ) {
        this(entityType, entityIdentifier, entityName, action, userName, timestamp, description, Map.of(), Map.of(), Map.of());
    }
}
