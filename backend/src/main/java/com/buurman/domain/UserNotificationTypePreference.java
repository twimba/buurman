package com.buurman.domain;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserNotificationTypePreference {

    private UUID id;
    private UUID userId;
    private NotificationType notificationType;
    private boolean emailEnabled = true;
    private boolean smsEnabled = false;
    private Instant createdAt;
    private Instant updatedAt;
}
