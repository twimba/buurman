package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification delivery record with status and engagement tracking")
public record NotificationResponse(
    @Schema(
            description = "Unique notification identifier",
            example = "ntf_01HZQX7V8B3K5M2N4P6R9T0W")
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
    boolean demoBlocked,
    Instant createdAt,
    Optional<Instant> statusUpdatedAt) {}
