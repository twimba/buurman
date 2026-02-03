package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

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

    public GeneratedReport() {
    }

    public GeneratedReport(UUID id, String identifier, UUID teamId, ReportType reportType,
                          ReportFormat format, String parameters, ReportStatus status,
                          Integer progress, String fileKey, String error, UUID createdBy,
                          Instant createdAt, Instant updatedAt, UUID updatedBy,
                          Instant completedAt, Instant expiresAt, Instant deletedAt) {
        this.id = id;
        this.identifier = identifier;
        this.teamId = teamId;
        this.reportType = reportType;
        this.format = format;
        this.parameters = parameters;
        this.status = status;
        this.progress = progress;
        this.fileKey = fileKey;
        this.error = error;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.completedAt = completedAt;
        this.expiresAt = expiresAt;
        this.deletedAt = deletedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public ReportFormat getFormat() {
        return format;
    }

    public void setFormat(ReportFormat format) {
        this.format = format;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
