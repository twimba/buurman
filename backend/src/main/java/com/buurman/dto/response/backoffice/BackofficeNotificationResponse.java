package com.buurman.dto.response.backoffice;

import java.time.Instant;

public record BackofficeNotificationResponse(
    String identifier,
    String teamIdentifier,
    String teamName,
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
    Instant statusUpdatedAt
) {}
