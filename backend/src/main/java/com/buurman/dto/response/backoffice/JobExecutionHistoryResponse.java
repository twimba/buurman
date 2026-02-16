package com.buurman.dto.response.backoffice;

public record JobExecutionHistoryResponse(
    String id,
    String jobName,
    String jobGroup,
    String startedAt,
    String endedAt,
    Long durationMs,
    String status,
    String errorMessage,
    String nodeId) {}
