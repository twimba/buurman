package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.RelationshipType;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactRelationshipResponse(
    Sid identifier,
    ContactSummary relatedContact,
    RelationshipType relationshipType,
    String displayLabel,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
