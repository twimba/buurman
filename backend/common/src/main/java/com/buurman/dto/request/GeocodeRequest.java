package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

public record GeocodeRequest(
    @NotBlank String street,
    @NotBlank String city,
    Optional<String> postalCode,
    @NotBlank String countryCode) {}
