package com.buurman.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type, BigDecimal areaValue, String areaUnit) {}
