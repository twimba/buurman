package com.buurman.dto.response;

import java.math.BigDecimal;

public record WwsCategoryBreakdown(
    String key,
    String name,
    String nameNl,
    BigDecimal points,
    String explanation) {}
