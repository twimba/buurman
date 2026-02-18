package com.buurman.dto.response;

import java.math.BigDecimal;

public record GeocodeResponse(BigDecimal latitude, BigDecimal longitude, String accuracy) {}
