package com.buurman.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.dto.response.RentPeriodResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractRentPeriodMapper {

  private final ContractRentComponentMapper componentMapper;

  public RentPeriodResponse toResponse(
      ContractRentPeriod period,
      Optional<BigDecimal> previousRentAmount,
      List<ContractRentComponent> components) {
    BigDecimal rentValue = period.getRentAmount().value();
    Optional<BigDecimal> percentageChange =
        previousRentAmount
            .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
            .map(
                p ->
                    rentValue
                        .subtract(p)
                        .divide(p, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));

    return new RentPeriodResponse(
        period.getIdentifier().orElseThrow(),
        rentValue,
        period.getRentAmount().currency(),
        period.getEffectiveFrom(),
        period.getEffectiveTo(),
        period.getNotes(),
        percentageChange,
        componentMapper.toResponses(components),
        period.getCreatedAt());
  }

  public List<RentPeriodResponse> toResponses(
      List<ContractRentPeriod> periods,
      Map<UUID, List<ContractRentComponent>> componentsByPeriodId) {
    // periods are ordered by effective_from DESC
    // percentageChange is vs the immediately preceding period (the one with earlier effective_from)
    return java.util.stream.IntStream.range(0, periods.size())
        .mapToObj(
            i -> {
              ContractRentPeriod current = periods.get(i);
              // The "previous" period is the next item in the list (since list is DESC)
              Optional<BigDecimal> previousAmount =
                  (i + 1 < periods.size())
                      ? Optional.of(periods.get(i + 1).getRentAmount().value())
                      : Optional.empty();
              List<ContractRentComponent> components =
                  componentsByPeriodId.getOrDefault(current.getId(), List.of());
              return toResponse(current, previousAmount, components);
            })
        .toList();
  }
}
