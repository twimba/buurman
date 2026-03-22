package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.RelationshipType;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record UpdateContactRelationshipRequest(
    @NotNull(message = "Relationship type is required") RelationshipType relationshipType,
    Optional<String> notes) {}
