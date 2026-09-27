package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;

/**
 * One row of an entity's communications timeline.
 *
 * <p>Deliberately not {@code NotificationResponse}: the body is omitted because a landlord does
 * not need the full rendered message in a timeline row, and {@code openCount}/{@code clickCount}
 * collapse to a single {@code opened} flag because the question this answers is "did it arrive
 * and was it read", not analytics.
 */
public record CommunicationResponse(
    Sid identifier,
    String notificationType,
    String channel,
    Optional<String> subject,
    Optional<String> recipientEmail,
    Optional<String> recipientPhone,
    String status,
    Optional<String> providerError,
    boolean opened,
    Optional<Instant> firstOpenedAt,
    Optional<Sid> resentFromIdentifier,
    Instant createdAt) {}
