package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.RelationshipType;
import com.buurman.domain.identifier.ContactIdentifier;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record CreateContactRelationshipRequest(
    @NotNull(message = "Target contact identifier is required")
        ContactIdentifier targetContactIdentifier,
    @NotNull(message = "Relationship type is required") RelationshipType relationshipType,
    Optional<String> notes) {}
