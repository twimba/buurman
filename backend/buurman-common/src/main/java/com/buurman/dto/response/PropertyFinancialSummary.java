package com.buurman.dto.response;

import java.math.BigDecimal;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertyFinancialSummary(
    PropertySummary property,
    BigDecimal income,
    BigDecimal expenses,
    BigDecimal netProfit,
    int occupancyDays) {}
