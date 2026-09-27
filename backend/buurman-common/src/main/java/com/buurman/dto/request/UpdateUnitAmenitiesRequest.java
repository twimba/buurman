package com.buurman.dto.request;

import java.util.List;

import com.buurman.domain.identifier.AmenityIdentifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Replaces the full set of amenities linked to a unit in one call. An empty list clears all
 * amenities.
 */
public record UpdateUnitAmenitiesRequest(
    @NotNull(message = "Amenity identifiers are required") @Size(max = 100, message = "At most 100 amenities can be linked to a unit") List<AmenityIdentifier> amenityIdentifiers) {}
