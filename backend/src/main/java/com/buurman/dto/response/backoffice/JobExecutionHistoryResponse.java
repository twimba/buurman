package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

public record JobExecutionHistoryResponse(
    String id,
    String jobName,
    String jobGroup,
    String startedAt,
    @Nullable String endedAt,
    @Nullable Long durationMs,
    String status,
    @Nullable String errorMessage,
    @Nullable String nodeId) {}
