package com.buurman.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Projection of an audit log record joined with user info. */
public record AuditLogEntry(
    UUID id,
    String entityType,
    UUID entityId,
    String action,
    LocalDateTime timestamp,
    @Nullable String changedFieldsJson,
    @Nullable String oldValuesJson,
    @Nullable String newValuesJson,
    @Nullable String firstName,
    @Nullable String lastName) {}
