package com.buurman.dto.request;

import java.util.Optional;

/**
 * Stub DTO for backward compatibility with the OpenAPI spec. The link-property endpoint is removed
 * in the contacts rework (BUUR-77) and will be deleted from the spec in Phase 4a.
 *
 * @deprecated Will be removed when the OpenAPI spec is updated.
 */
@Deprecated(forRemoval = true)
public record LinkContactToPropertyRequest(
    Optional<String> propertyIdentifier, Optional<String> movedInAt) {}
