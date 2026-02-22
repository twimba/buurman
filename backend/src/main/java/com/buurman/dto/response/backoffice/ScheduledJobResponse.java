package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

public record ScheduledJobResponse(
    String jobName,
    String jobGroup,
    String jobClass,
    @Nullable String triggerName,
    @Nullable String triggerGroup,
    @Nullable String triggerType,
    @Nullable String scheduleExpression,
    String triggerState,
    @Nullable String nextFireTime,
    @Nullable String previousFireTime) {}
