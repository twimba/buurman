package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;

@Generated
public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit) {}
