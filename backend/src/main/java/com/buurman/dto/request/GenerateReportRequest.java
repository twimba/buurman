package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
    @NotNull ReportType reportType,
    @NotNull ReportFormat format,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Nullable List<UUID> propertyIds,
    @Nullable Boolean includeDocuments) {
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
