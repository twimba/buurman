package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import com.buurman.util.Generated;

@Generated
public record GeocodeRequest(
    @NotBlank String street,
    @NotBlank String city,
    Optional<String> postalCode,
    @NotBlank String countryCode) {}
