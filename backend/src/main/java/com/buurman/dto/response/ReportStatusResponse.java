package com.buurman.dto.response;

import java.time.Instant;

public record ReportStatusResponse(
        String reportId,
        String identifier,
        ReportStatus status,
        Integer progress,
        String downloadUrl,
        Instant expiresAt,
        String error
) {
    public enum ReportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
