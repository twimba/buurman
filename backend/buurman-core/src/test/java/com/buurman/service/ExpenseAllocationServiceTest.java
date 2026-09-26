package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseAllocationService.computeAllocations")
class ExpenseAllocationServiceTest {

  private final ExpenseAllocationService service = new ExpenseAllocationService(null, null, null);

  @Test
  @DisplayName("splits a 1200.00 expense equally across 4 units as 300.00 each")
  void splitsEquallyWithoutRemainder() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("1200.00"), AllocationBasis.EQUAL, units(null, null, null, null));

    assertThat(allocations).hasSize(4);
    assertThat(allocations)
        .allSatisfy(a -> assertThat(a.getAmount().value()).isEqualByComparingTo("300.00"));
    assertThat(sum(allocations)).isEqualByComparingTo("1200.00");
  }

  @Test
  @DisplayName("distributes an indivisible remainder by largest remainder, summing exactly")
  void distributesRemainderExactly() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("100.00"), AllocationBasis.EQUAL, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("100.00");
    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactlyInAnyOrder("33.34", "33.33", "33.33");
  }

  @Test
  @DisplayName("splits by area under AREA basis")
  void splitsByArea() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("900.00"),
            AllocationBasis.AREA,
            units(new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50")));

    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactly("450.00", "225.00", "225.00");
    assertThat(sum(allocations)).isEqualByComparingTo("900.00");
  }

  @Test
  @DisplayName("falls back to EQUAL and records it when no unit has an area")
  void fallsBackToEqualWhenNoAreas() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("300.00"), AllocationBasis.AREA, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("300.00");
    assertThat(allocations)
        .allSatisfy(a -> assertThat(a.getBasis()).isEqualTo(AllocationBasis.EQUAL));
  }

  @Test
  @DisplayName("treats a unit with no area as a zero share when siblings have areas")
  void treatsMissingAreaAsZeroShare() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("300.00"), AllocationBasis.AREA, units(new BigDecimal("100"), null));

    assertThat(allocations.get(0).getAmount().value()).isEqualByComparingTo("300.00");
    assertThat(allocations.get(1).getAmount().value()).isEqualByComparingTo("0.00");
    assertThat(sum(allocations)).isEqualByComparingTo("300.00");
  }

  @Test
  @DisplayName("sums exactly for a negative amount, such as a credit note")
  void handlesNegativeAmounts() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            money("-100.00"), AllocationBasis.EQUAL, units(null, null, null));

    assertThat(sum(allocations)).isEqualByComparingTo("-100.00");
    assertThat(allocations).allSatisfy(a -> assertThat(a.getAmount().value()).isNegative());
  }

  @Test
  @DisplayName("gives every allocation row the expense's own currency")
  void inheritsExpenseCurrency() {
    List<ExpenseAllocation> allocations =
        service.computeAllocations(
            new MoneyAmount(new BigDecimal("500.00"), "SEK"),
            AllocationBasis.EQUAL,
            units(null, null));

    assertThat(allocations).allSatisfy(a -> assertThat(a.getAmount().currency()).isEqualTo("SEK"));
  }

  @Test
  @DisplayName("splits by allocation_share under CUSTOM basis")
  void splitsByCustomShare() {
    List<Unit> unitList = units(null, null);
    unitList.get(0).setAllocationShare(Optional.of(new BigDecimal("70")));
    unitList.get(1).setAllocationShare(Optional.of(new BigDecimal("30")));

    List<ExpenseAllocation> allocations =
        service.computeAllocations(money("1000.00"), AllocationBasis.CUSTOM, unitList);

    assertThat(allocations)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactly("700.00", "300.00");
  }

  @Test
  @DisplayName("returns no allocations for an empty unit list")
  void handlesNoUnits() {
    assertThat(service.computeAllocations(money("100.00"), AllocationBasis.EQUAL, List.of()))
        .isEmpty();
  }

  private static MoneyAmount money(String value) {
    return new MoneyAmount(new BigDecimal(value), "EUR");
  }

  private static BigDecimal sum(List<ExpenseAllocation> allocations) {
    return allocations.stream()
        .map(a -> a.getAmount().value())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static List<Unit> units(@Nullable BigDecimal... areas) {
    java.util.List<Unit> result = new java.util.ArrayList<>();
    for (int i = 0; i < areas.length; i++) {
      result.add(
          Unit.builder()
              .id(UUID.randomUUID())
              .teamId(UUID.randomUUID())
              .propertyId(UUID.randomUUID())
              .unitNumber(String.valueOf(i + 1))
              .unitType(UnitType.APARTMENT)
              .status(UnitStatus.VACANT)
              .areaValue(Optional.ofNullable(areas[i]))
              .sortOrder(i)
              .build());
    }
    return result;
  }
}
