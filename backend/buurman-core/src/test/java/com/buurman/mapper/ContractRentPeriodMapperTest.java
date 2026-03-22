package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Sid;
import com.buurman.dto.response.RentPeriodResponse;
import com.buurman.util.MoneyAmount;

@DisplayName("ContractRentPeriodMapper")
class ContractRentPeriodMapperTest {

  private final ContractRentPeriodMapper mapper =
      new ContractRentPeriodMapper(new ContractRentComponentMapper());

  private static final Sid PERIOD_ID = Sid.of("CRP01HQJK4B2X5M3N7P8Q9R0S1T2");

  @Nested
  @DisplayName("toResponse")
  class ToResponseTest {

    @Test
    @DisplayName("maps basic fields correctly")
    void mapsBasicFields() {
      ContractRentPeriod period =
          createPeriod(
              new BigDecimal("1200.00"),
              "EUR",
              LocalDate.of(2026, 1, 1),
              Optional.of(LocalDate.of(2026, 12, 31)),
              Optional.of("Initial period"));

      RentPeriodResponse response = mapper.toResponse(period, Optional.empty(), List.of());

      assertThat(response.identifier()).isEqualTo(PERIOD_ID);
      assertThat(response.rentAmount()).isEqualByComparingTo(new BigDecimal("1200.00"));
      assertThat(response.currency()).isEqualTo("EUR");
      assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
      assertThat(response.effectiveTo()).contains(LocalDate.of(2026, 12, 31));
      assertThat(response.notes()).contains("Initial period");
    }

    @Test
    @DisplayName("calculates positive percentage change")
    void calculatesPositivePercentageChange() {
      ContractRentPeriod period =
          createPeriod(
              new BigDecimal("1100.00"),
              "EUR",
              LocalDate.of(2027, 1, 1),
              Optional.empty(),
              Optional.empty());

      RentPeriodResponse response =
          mapper.toResponse(period, Optional.of(new BigDecimal("1000.00")), List.of());

      assertThat(response.percentageChange()).isPresent();
      assertThat(response.percentageChange().get()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("calculates negative percentage change")
    void calculatesNegativePercentageChange() {
      ContractRentPeriod period =
          createPeriod(
              new BigDecimal("900.00"),
              "EUR",
              LocalDate.of(2027, 1, 1),
              Optional.empty(),
              Optional.empty());

      RentPeriodResponse response =
          mapper.toResponse(period, Optional.of(new BigDecimal("1000.00")), List.of());

      assertThat(response.percentageChange()).isPresent();
      assertThat(response.percentageChange().get()).isEqualByComparingTo(new BigDecimal("-10.00"));
    }

    @Test
    @DisplayName("returns empty percentage when no previous amount")
    void returnsEmptyPercentageWhenNoPrevious() {
      ContractRentPeriod period =
          createPeriod(
              new BigDecimal("1200.00"),
              "EUR",
              LocalDate.of(2026, 1, 1),
              Optional.empty(),
              Optional.empty());

      RentPeriodResponse response = mapper.toResponse(period, Optional.empty(), List.of());

      assertThat(response.percentageChange()).isEmpty();
    }

    @Test
    @DisplayName("returns empty percentage when previous amount is zero")
    void returnsEmptyPercentageWhenPreviousIsZero() {
      ContractRentPeriod period =
          createPeriod(
              new BigDecimal("1200.00"),
              "EUR",
              LocalDate.of(2026, 1, 1),
              Optional.empty(),
              Optional.empty());

      RentPeriodResponse response =
          mapper.toResponse(period, Optional.of(BigDecimal.ZERO), List.of());

      assertThat(response.percentageChange()).isEmpty();
    }
  }

  @Nested
  @DisplayName("toResponses")
  class ToResponsesTest {

    @Test
    @DisplayName("computes percentage change between consecutive periods")
    void computesPercentageChangeBetweenPeriods() {
      // List is DESC by effectiveFrom
      List<ContractRentPeriod> periods =
          List.of(
              createPeriod(
                  new BigDecimal("1210.00"),
                  "EUR",
                  LocalDate.of(2028, 1, 1),
                  Optional.empty(),
                  Optional.empty()),
              createPeriod(
                  new BigDecimal("1100.00"),
                  "EUR",
                  LocalDate.of(2027, 1, 1),
                  Optional.empty(),
                  Optional.empty()),
              createPeriod(
                  new BigDecimal("1000.00"),
                  "EUR",
                  LocalDate.of(2026, 1, 1),
                  Optional.empty(),
                  Optional.empty()));

      List<RentPeriodResponse> responses = mapper.toResponses(periods, Map.of());

      assertThat(responses).hasSize(3);
      // First (newest): 1210 vs 1100 = +10%
      assertThat(responses.get(0).percentageChange().get())
          .isEqualByComparingTo(new BigDecimal("10.00"));
      // Second: 1100 vs 1000 = +10%
      assertThat(responses.get(1).percentageChange().get())
          .isEqualByComparingTo(new BigDecimal("10.00"));
      // Third (oldest): no predecessor
      assertThat(responses.get(2).percentageChange()).isEmpty();
    }

    @Test
    @DisplayName("empty list returns empty list")
    void emptyListReturnsEmpty() {
      List<RentPeriodResponse> responses = mapper.toResponses(List.of(), Map.of());

      assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("single period has no percentage change")
    void singlePeriodNoPercentage() {
      List<ContractRentPeriod> periods =
          List.of(
              createPeriod(
                  new BigDecimal("1000.00"),
                  "EUR",
                  LocalDate.of(2026, 1, 1),
                  Optional.empty(),
                  Optional.empty()));

      List<RentPeriodResponse> responses = mapper.toResponses(periods, Map.of());

      assertThat(responses).hasSize(1);
      assertThat(responses.get(0).percentageChange()).isEmpty();
    }
  }

  private ContractRentPeriod createPeriod(
      BigDecimal amount,
      String currency,
      LocalDate effectiveFrom,
      Optional<LocalDate> effectiveTo,
      Optional<String> notes) {
    return ContractRentPeriod.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(PERIOD_ID))
        .teamId(UUID.randomUUID())
        .contractId(UUID.randomUUID())
        .rentAmount(MoneyAmount.of(amount, currency))
        .effectiveFrom(effectiveFrom)
        .effectiveTo(effectiveTo)
        .notes(notes)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(UUID.randomUUID())
        .updatedBy(UUID.randomUUID())
        .build();
  }
}
