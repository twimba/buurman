package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

public record GeocodeResponse(
    Optional<BigDecimal> latitude, Optional<BigDecimal> longitude, Optional<String> accuracy) {}
