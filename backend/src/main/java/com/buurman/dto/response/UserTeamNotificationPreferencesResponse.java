package com.buurman.dto.response;

import java.util.List;
import java.util.UUID;

public record UserTeamNotificationPreferencesResponse(
    UUID teamId,
    boolean paymentReminders,
    boolean contractExpiryAlerts,
    boolean newMemberNotifications,
    boolean weeklySummary,
    List<String> preferredChannels
) {}
