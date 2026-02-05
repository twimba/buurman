package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PropertyOutdoorAreaRequest(
        @NotBlank(message = "Type is required")
        String type,

        BigDecimal areaValue,

        String areaUnit
) {}
