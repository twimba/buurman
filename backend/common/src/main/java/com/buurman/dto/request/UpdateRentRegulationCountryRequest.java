package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record UpdateRentRegulationCountryRequest(
    @NotBlank @Size(max = 100) String countryName,
    boolean hasRegionalRegulations,
    Optional<String> summary) {}
