package com.buurman.service.notification;

import java.util.UUID;

public record NotificationSendRequest(
        UUID notificationId,
        String recipientEmail,
        String recipientPhone,
        String subject,
        String body,
        String fromEmail,
        String fromName
) {}
