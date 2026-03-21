package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import com.buurman.util.Generated;

@Generated
public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit) {}
