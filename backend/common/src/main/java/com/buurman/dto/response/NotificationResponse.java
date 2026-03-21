package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record NotificationResponse(
    Sid identifier,
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
    Optional<Sid> resentFromIdentifier,
    Optional<String> resendReason,
    boolean demoBlocked,
    Instant createdAt,
    Optional<Instant> statusUpdatedAt) {}
