package com.buurman.dto.response.backoffice;

public record ScheduledJobResponse(
        String jobName,
        String jobGroup,
        String jobClass,
        String triggerName,
        String triggerGroup,
        String triggerType,
        String scheduleExpression,
        String triggerState,
        String nextFireTime,
        String previousFireTime
) {}
