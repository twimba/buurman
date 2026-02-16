package com.buurman.dto.response;

import java.math.BigDecimal;

public record PropertyFinancialSummary(
    PropertySummary property,
    BigDecimal income,
    BigDecimal expenses,
    BigDecimal netProfit,
    int occupancyDays) {}
