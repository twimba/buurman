package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GeocodeRequest(
    @NotBlank String street, @NotBlank String city, String postalCode, @NotBlank String country) {}
