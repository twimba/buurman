package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record ReportStatusResponse(
    String reportId,
    Ulid identifier,
    ReportStatus status,
    Optional<Integer> progress,
    Optional<String> downloadUrl,
    Optional<Instant> expiresAt,
    Optional<String> error) {
  public enum ReportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }
}
