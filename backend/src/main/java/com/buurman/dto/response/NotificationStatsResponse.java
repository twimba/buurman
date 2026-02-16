package com.buurman.dto.response;

import java.util.Map;

public record NotificationStatsResponse(
    long totalCount,
    long pendingCount,
    long sentCount,
    long deliveredCount,
    long failedCount,
    Map<String, Long> byChannel) {}
