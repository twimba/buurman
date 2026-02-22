package com.buurman.dto.request;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;

public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type,
    @Nullable BigDecimal areaValue,
    @Nullable String areaUnit) {}
