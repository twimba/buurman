package com.buurman.dto.response;

import java.util.Optional;

public record ResidentialDetailsResponse(
    Optional<Integer> bedrooms,
    Optional<Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {}
