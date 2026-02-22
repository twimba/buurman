package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
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
  private String identifier;
  private UUID teamId;
  private ReportType reportType;
  private ReportFormat format;
  private @Nullable String parameters; // JSONB stored as string
  private ReportStatus status;
  private @Nullable Integer progress;
  private @Nullable String fileKey;
  private @Nullable String error;
  private UUID createdBy;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID updatedBy;
  private @Nullable Instant completedAt;
  private @Nullable Instant expiresAt;
  private @Nullable Instant deletedAt;
}
