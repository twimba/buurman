package com.buurman.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.ContractRentPeriod;
import com.buurman.dto.response.RentPeriodResponse;

@Component
public class ContractRentPeriodMapper {

  public RentPeriodResponse toResponse(
      ContractRentPeriod period, @Nullable BigDecimal previousRentAmount) {
    BigDecimal percentageChange = null;
    if (previousRentAmount != null && previousRentAmount.compareTo(BigDecimal.ZERO) > 0) {
      percentageChange =
          period
              .getRentAmount()
              .subtract(previousRentAmount)
              .divide(previousRentAmount, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
    }

    return new RentPeriodResponse(
        period.getIdentifier(),
        period.getRentAmount(),
        period.getCurrency(),
        period.getEffectiveFrom(),
        period.getEffectiveTo(),
        period.getNotes(),
        percentageChange,
        period.getCreatedAt());
  }

  public List<RentPeriodResponse> toResponses(List<ContractRentPeriod> periods) {
    // periods are ordered by effective_from DESC
    // percentageChange is vs the immediately preceding period (the one with earlier effective_from)
    return java.util.stream.IntStream.range(0, periods.size())
        .mapToObj(
            i -> {
              ContractRentPeriod current = periods.get(i);
              // The "previous" period is the next item in the list (since list is DESC)
              BigDecimal previousAmount =
                  (i + 1 < periods.size()) ? periods.get(i + 1).getRentAmount() : null;
              return toResponse(current, previousAmount);
            })
        .toList();
  }
}
