package com.buurman.dto.response.backoffice;

import java.util.Map;

public record BackofficeDashboardResponse(
    long totalTeams,
    long totalUsers,
    long disabledUsers,
    long totalNotifications,
    long pendingNotifications,
    long failedNotifications,
    long deliveredNotifications,
    Map<String, Long> notificationsByChannel) {}
