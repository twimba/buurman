package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.RelationshipType;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record UpdateContactRelationshipRequest(
    @NotNull(message = "Relationship type is required") RelationshipType relationshipType,
    Optional<String> notes) {}
