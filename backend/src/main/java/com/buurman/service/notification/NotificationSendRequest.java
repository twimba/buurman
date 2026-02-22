package com.buurman.service.notification;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record NotificationSendRequest(
    @Nullable UUID notificationId,
    @Nullable String recipientEmail,
    @Nullable String recipientPhone,
    @Nullable String subject,
    String body,
    @Nullable String fromEmail,
    @Nullable String fromName) {}
