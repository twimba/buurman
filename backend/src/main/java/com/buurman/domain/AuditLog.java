package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

  private UUID id;
  private UUID teamId;
  private String entityType;
  private UUID entityId;
  private Action action;
  private Map<String, Object> changedFields;
  private Map<String, Object> oldValues;
  private Map<String, Object> newValues;
  private UUID userId;
  private Instant timestamp;

  public enum Action {
    CREATE,
    UPDATE,
    DELETE,
    RESTORE
  }
}
