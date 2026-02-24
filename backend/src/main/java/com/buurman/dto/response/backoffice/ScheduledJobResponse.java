package com.buurman.dto.response.backoffice;

import java.util.Optional;

public record ScheduledJobResponse(
    String jobName,
    String jobGroup,
    String jobClass,
    Optional<String> triggerName,
    Optional<String> triggerGroup,
    Optional<String> triggerType,
    Optional<String> scheduleExpression,
    String triggerState,
    Optional<String> nextFireTime,
    Optional<String> previousFireTime) {}
