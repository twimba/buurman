package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class UserPreferences {

    private UUID id;
    private UUID userId;
    private String theme;
    private String language;
    private String timezone;
    private String dateFormat;
    private String currencyFormat;
    private boolean emailNotifications;
    private boolean inAppNotifications;
    private Instant createdAt;
    private Instant updatedAt;

    public UserPreferences() {
        // Set defaults
        this.theme = "system";
        this.language = "en";
        this.timezone = "UTC";
        this.dateFormat = "DD/MM/YYYY";
        this.currencyFormat = "EUR";
        this.emailNotifications = true;
        this.inAppNotifications = true;
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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getCurrencyFormat() {
        return currencyFormat;
    }

    public void setCurrencyFormat(String currencyFormat) {
        this.currencyFormat = currencyFormat;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isInAppNotifications() {
        return inAppNotifications;
    }

    public void setInAppNotifications(boolean inAppNotifications) {
        this.inAppNotifications = inAppNotifications;
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
