package com.buurman.dto.response.backoffice;

import java.util.Map;
import com.buurman.util.Generated;

@Generated
public record BackofficeDashboardResponse(
    long totalTeams,
    long totalUsers,
    long disabledUsers,
    long totalNotifications,
    long pendingNotifications,
    long failedNotifications,
    long deliveredNotifications,
    Map<String, Long> notificationsByChannel) {}
