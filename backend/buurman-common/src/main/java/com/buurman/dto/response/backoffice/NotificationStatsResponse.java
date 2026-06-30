package com.buurman.dto.response.backoffice;

import java.util.Map;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record NotificationStatsResponse(
    long total, Map<String, Long> byStatus, Map<String, Long> byChannel) {}
