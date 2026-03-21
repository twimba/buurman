package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.InteractionType;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContactNoteResponse(
    Sid identifier,
    InteractionType interactionType,
    Optional<String> subject,
    String body,
    Instant occurredAt,
    Optional<LocalDate> followUpDate,
    boolean followUpReminderSent,
    boolean pinned,
    Optional<String> createdByName,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
