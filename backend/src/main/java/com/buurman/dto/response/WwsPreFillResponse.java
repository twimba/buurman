package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

public record WwsPreFillResponse(
    Optional<BigDecimal> surfaceAreaSqm,
    Optional<Integer> numberOfRooms,
    Optional<Integer> numberOfHeatedRooms,
    Optional<String> energyLabel,
    Optional<BigDecimal> outdoorSpaceSqm,
    Optional<String> parkingType,
    Optional<Integer> parkingSpaces,
    Optional<Integer> accessibilityFeatures,
    Optional<String> propertyAddress) {}
