package com.buurman.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GenerateReportRequest(
        @NotNull ReportType reportType,
        @NotNull ReportFormat format,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        List<UUID> propertyIds,
        Boolean includeDocuments
) {
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
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
        PDF, EXCEL, CSV
    }
}
