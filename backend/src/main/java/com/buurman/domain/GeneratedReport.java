package com.buurman.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
        PDF, EXCEL, CSV
    }

    public enum ReportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    private UUID id;
    private String identifier;
    private UUID teamId;
    private ReportType reportType;
    private ReportFormat format;
    private String parameters; // JSONB stored as string
    private ReportStatus status;
    private Integer progress;
    private String fileKey;
    private String error;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant completedAt;
    private Instant expiresAt;
    private Instant deletedAt;
}
