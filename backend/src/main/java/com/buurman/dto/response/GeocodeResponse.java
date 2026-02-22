package com.buurman.dto.response;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record GeocodeResponse(
    @Nullable BigDecimal latitude, @Nullable BigDecimal longitude, @Nullable String accuracy) {}
