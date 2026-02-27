package com.buurman.dto.response;

import java.util.Map;

public record NotificationStatsResponse(
    long totalCount,
    long pendingCount,
    long sentCount,
    long deliveredCount,
    long failedCount,
    long demoBlockedCount,
    Map<String, Long> byChannel) {}
