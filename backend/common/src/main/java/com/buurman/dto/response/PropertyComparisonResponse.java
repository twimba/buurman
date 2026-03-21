package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import com.buurman.util.Generated;

@Generated
public record PropertyComparisonResponse(List<PropertyData> properties, String currency) {
  public record PropertyData(
      PropertySummary property, BigDecimal income, BigDecimal expenses, BigDecimal netProfit) {}
}
