package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.RelationshipType;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record CreateContactRelationshipRequest(
    @NotNull(message = "Target contact identifier is required") ContactIdentifier targetContactIdentifier,
    @NotNull(message = "Relationship type is required") RelationshipType relationshipType,
    Optional<String> notes) {}
