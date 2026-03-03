package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record BackofficeNotificationResponse(
    Ulid identifier,
    Optional<Ulid> teamIdentifier,
    Optional<String> teamName,
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
    Optional<Ulid> resentFromIdentifier,
    Optional<String> resendReason,
    boolean demoBlocked,
    Instant createdAt,
    Optional<Instant> statusUpdatedAt) {}
