package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record NotificationResponse(
    String identifier,
    String notificationType,
    String channel,
    Optional<String> subject,
    Optional<String> body,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    String status,
    Optional<String> providerStatus,
    Optional<String> providerError,
    int openCount,
    int clickCount,
    Optional<Instant> firstOpenedAt,
    Optional<Instant> firstClickedAt,
    Optional<String> resentFromIdentifier,
    Optional<String> resendReason,
    Instant createdAt,
    Optional<Instant> statusUpdatedAt) {}
