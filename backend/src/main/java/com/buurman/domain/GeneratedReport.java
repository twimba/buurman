package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {

  public enum ReportType {
    INCOME_STATEMENT,
    EXPENSE_REPORT,
    PROPERTY_REPORT,
    TAX_SUMMARY,
    TRANSACTION_HISTORY
  }

  public enum ReportFormat {
    PDF,
    EXCEL,
    CSV
  }

  public enum ReportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }

  private UUID id;
  @Builder.Default private Optional<Ulid> identifier = Optional.empty();
  private UUID teamId;
  private ReportType reportType;
  private ReportFormat format;
  @Builder.Default private Optional<String> parameters = Optional.empty(); // JSONB stored as string
  private ReportStatus status;
  @Builder.Default private Optional<Integer> progress = Optional.empty();
  @Builder.Default private Optional<String> fileKey = Optional.empty();
  @Builder.Default private Optional<String> error = Optional.empty();
  private UUID createdBy;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> completedAt = Optional.empty();
  @Builder.Default private Optional<Instant> expiresAt = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
