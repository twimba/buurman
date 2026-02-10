package com.buurman.dto.request;

import java.util.List;

public record UpdateTeamNotificationPreferencesRequest(
    Boolean paymentReminders,
    Boolean contractExpiryAlerts,
    Boolean newMemberNotifications,
    Boolean weeklySummary,
    List<String> preferredChannels
) {}
