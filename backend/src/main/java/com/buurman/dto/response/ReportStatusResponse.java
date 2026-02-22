package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record ReportStatusResponse(
    String reportId,
    String identifier,
    ReportStatus status,
    @Nullable Integer progress,
    @Nullable String downloadUrl,
    @Nullable Instant expiresAt,
    @Nullable String error) {
  public enum ReportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }
}
