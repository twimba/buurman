package com.buurman.dto.response;

import java.time.Instant;

public record NotificationResponse(
    String identifier,
    String notificationType,
    String channel,
    String subject,
    String body,
    String recipientEmail,
    String recipientPhone,
    String status,
    String providerStatus,
    String providerError,
    String resentFromIdentifier,
    String resendReason,
    Instant createdAt,
    Instant statusUpdatedAt) {}
