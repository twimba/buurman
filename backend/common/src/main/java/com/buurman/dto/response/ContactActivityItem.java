package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.InteractionType;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContactActivityItem(
    String eventType,
    Instant occurredAt,
    String description,
    Optional<Sid> relatedEntityIdentifier,
    Optional<String> relatedEntityType,
    Optional<Sid> noteIdentifier,
    Optional<InteractionType> interactionType,
    Optional<String> noteBody,
    Optional<String> noteSubject,
    Optional<Boolean> pinned,
    Optional<String> createdByName) {}
