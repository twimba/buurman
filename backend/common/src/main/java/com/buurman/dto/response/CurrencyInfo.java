package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CurrencyInfo(String code, String name, String symbol, int fractionalDigits) {}
