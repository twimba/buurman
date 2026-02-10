package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class UserTeamNotificationPreferences {

    private UUID id;
    private UUID userId;
    private UUID teamId;
    private boolean paymentReminders;
    private boolean contractExpiryAlerts;
    private boolean newMemberNotifications;
    private boolean weeklySummary;
    private String preferredChannels;
    private Instant createdAt;
    private Instant updatedAt;

    public UserTeamNotificationPreferences() {
        // Set defaults
        this.paymentReminders = true;
        this.contractExpiryAlerts = true;
        this.newMemberNotifications = true;
        this.weeklySummary = true;
        this.preferredChannels = "EMAIL";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public boolean isPaymentReminders() {
        return paymentReminders;
    }

    public void setPaymentReminders(boolean paymentReminders) {
        this.paymentReminders = paymentReminders;
    }

    public boolean isContractExpiryAlerts() {
        return contractExpiryAlerts;
    }

    public void setContractExpiryAlerts(boolean contractExpiryAlerts) {
        this.contractExpiryAlerts = contractExpiryAlerts;
    }

    public boolean isNewMemberNotifications() {
        return newMemberNotifications;
    }

    public void setNewMemberNotifications(boolean newMemberNotifications) {
        this.newMemberNotifications = newMemberNotifications;
    }

    public boolean isWeeklySummary() {
        return weeklySummary;
    }

    public void setWeeklySummary(boolean weeklySummary) {
        this.weeklySummary = weeklySummary;
    }

    public String getPreferredChannels() {
        return preferredChannels;
    }

    public void setPreferredChannels(String preferredChannels) {
        this.preferredChannels = preferredChannels;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
