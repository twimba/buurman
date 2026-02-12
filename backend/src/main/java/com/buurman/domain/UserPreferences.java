package com.buurman.domain;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserPreferences {

    private UUID id;
    private UUID userId;
    private String theme = "system";
    private String language = "en";
    private String timezone = "UTC";
    private String dateFormat = "DD/MM/YYYY";
    private String currencyFormat = "EUR";
    private boolean emailNotifications = true;
    private boolean inAppNotifications = true;
    private boolean smsNotifications = false;
    private Instant createdAt;
    private Instant updatedAt;
}
