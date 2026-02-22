package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record NotificationResponse(
    String identifier,
    String notificationType,
    String channel,
    @Nullable String subject,
    @Nullable String body,
    @Nullable String recipientEmail,
    @Nullable String recipientPhone,
    String status,
    @Nullable String providerStatus,
    @Nullable String providerError,
    int openCount,
    int clickCount,
    @Nullable Instant firstOpenedAt,
    @Nullable Instant firstClickedAt,
    @Nullable String resentFromIdentifier,
    @Nullable String resendReason,
    Instant createdAt,
    @Nullable Instant statusUpdatedAt) {}
