package com.buurman.dto.response.backoffice;

import java.util.Optional;
import com.buurman.util.Generated;

@Generated
public record JobExecutionHistoryResponse(
    String id,
    String jobName,
    String jobGroup,
    String startedAt,
    Optional<String> endedAt,
    Optional<Long> durationMs,
    String status,
    Optional<String> errorMessage,
    Optional<String> nodeId) {}
