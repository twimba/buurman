package com.buurman.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Projection of an audit log record joined with user info. */
public record AuditLogEntry(
    UUID id,
    String entityType,
    UUID entityId,
    String action,
    LocalDateTime timestamp,
    Optional<String> changedFieldsJson,
    Optional<String> oldValuesJson,
    Optional<String> newValuesJson,
    Optional<String> firstName,
    Optional<String> lastName) {}
