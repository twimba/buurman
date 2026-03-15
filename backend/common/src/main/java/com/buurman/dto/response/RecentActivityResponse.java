package com.buurman.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.buurman.domain.Sid;

public record RecentActivityResponse(
    String entityType,
    Sid entityIdentifier,
    String entityName,
    String action,
    Optional<String> userName,
    Instant timestamp,
    Optional<String> description,
    Map<String, Object> changedFields,
    Map<String, Object> oldValues,
    Map<String, Object> newValues,
    Optional<String> impersonatedBy) {
  public RecentActivityResponse(
      String entityType,
      Sid entityIdentifier,
      String entityName,
      String action,
      String userName,
      Instant timestamp,
      String description) {
    this(
        entityType,
        entityIdentifier,
        entityName,
        action,
        Optional.of(userName),
        timestamp,
        Optional.of(description),
        Map.of(),
        Map.of(),
        Map.of(),
        Optional.empty());
  }
}
