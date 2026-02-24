package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
    @NotNull ReportType reportType,
    @NotNull ReportFormat format,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Optional<List<UUID>> propertyIds,
    Optional<Boolean> includeDocuments) {
  public GenerateReportRequest {
    propertyIds = Objects.requireNonNullElse(propertyIds, Optional.empty());
    includeDocuments = Objects.requireNonNullElse(includeDocuments, Optional.empty());
  }

  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

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
}
