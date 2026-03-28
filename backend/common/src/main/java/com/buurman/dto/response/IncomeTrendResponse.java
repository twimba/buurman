package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record IncomeTrendResponse(List<DataPoint> dataPoints, String currency) {
  public record DataPoint(
      String period, BigDecimal income, BigDecimal expenses, BigDecimal netProfit) {}
}
