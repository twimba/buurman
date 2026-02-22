package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;

public record GeocodeRequest(
    @NotBlank String street,
    @NotBlank String city,
    @Nullable String postalCode,
    @NotBlank String country) {}
