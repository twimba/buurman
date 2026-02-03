package com.buurman.dto.request;

public record UpdateTeamNotificationPreferencesRequest(
    Boolean paymentReminders,
    Boolean contractExpiryAlerts,
    Boolean newMemberNotifications,
    Boolean weeklySummary
) {}
