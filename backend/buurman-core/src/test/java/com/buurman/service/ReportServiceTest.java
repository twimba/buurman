package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.TeamRole;
import com.buurman.dto.response.FinancialOverviewResponse;
import com.buurman.dto.response.OccupancyTrendResponse;
import com.buurman.dto.response.PropertyFinancialSummary;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService")
class ReportServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneOffset.UTC);

  @Mock private PaymentRepository paymentRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private ContractTerminationRepository contractTerminationRepository;
  @Mock private PropertyMapper propertyMapper;
  @Mock private TeamService teamService;

  private ReportService service;
  private UserPrincipal principal;
  private UUID propertyId;
  private Contract underNotice;

  @BeforeEach
  void setUp() {
    service =
        new ReportService(
            paymentRepository,
            expenseRepository,
            propertyRepository,
            contractRepository,
            contractExtensionRepository,
            contractTerminationRepository,
            propertyMapper,
            teamService,
            CLOCK);
    principal =
        new UserPrincipal(
            UUID.randomUUID(),
            "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
            "kc-123",
            "user@example.com",
            "John Doe",
            TEAM_ID,
            "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
            TeamRole.TEAM_ADMIN,
            true,
            true);
    propertyId = UUID.randomUUID();
    underNotice =
        Contract.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .unitId(UUID.randomUUID())
            .startDate(LocalDate.of(2025, 1, 1))
            .endDate(Optional.of(LocalDate.of(2026, 6, 30)))
            .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
            .status(Contract.ContractStatus.NOTICE_GIVEN)
            .build();
    when(contractRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(underNotice));
    when(contractExtensionRepository.findByContractIdsAndTeamId(any(), eq(TEAM_ID)))
        .thenReturn(List.of());
  }

  @Test
  @DisplayName("occupancy trend counts a NOTICE_GIVEN contract as an occupied unit")
  void occupancyTrendCountsNoticeGiven() {
    when(propertyRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(new Property()));

    OccupancyTrendResponse trend = service.getOccupancyTrend(1, principal);

    assertThat(trend.dataPoints())
        .singleElement()
        .satisfies(p -> assertThat(p.occupiedUnits()).isEqualTo(1));
  }

  @Test
  @DisplayName("date-range occupancy trend counts a NOTICE_GIVEN contract as an occupied unit")
  void occupancyTrendByDateRangeCountsNoticeGiven() {
    when(propertyRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(new Property()));

    OccupancyTrendResponse trend =
        service.getOccupancyTrendByDateRange(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal);

    assertThat(trend.dataPoints())
        .singleElement()
        .satisfies(p -> assertThat(p.occupiedUnits()).isEqualTo(1));
  }

  @Test
  @DisplayName("financial overview counts a NOTICE_GIVEN contract's days as occupied")
  void financialOverviewCountsNoticeGivenOccupancyDays() {
    LocalDate start = LocalDate.of(2026, 3, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);
    Property property = new Property();
    property.setId(propertyId);
    when(paymentRepository.findByDateRange(start, end, TEAM_ID))
        .thenReturn(
            List.of(
                Payment.builder()
                    .id(UUID.randomUUID())
                    .contractId(underNotice.getId())
                    .amount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                    .status(Payment.PaymentStatus.PAID)
                    .build()));
    when(expenseRepository.findByDateRange(start, end, TEAM_ID)).thenReturn(List.of());
    when(propertyRepository.findByIdsAndTeamId(any(), eq(TEAM_ID))).thenReturn(List.of(property));

    FinancialOverviewResponse overview =
        service.getFinancialOverview(start, end, null, "EUR", principal);

    assertThat(overview.income().byProperty())
        .singleElement()
        .extracting(PropertyFinancialSummary::occupancyDays)
        .isEqualTo(31);
  }

  @Test
  @DisplayName(
      "financial overview stops a NOTICE_GIVEN contract's occupancy days at the termination's "
          + "effective end date, not the contract's own end date")
  void financialOverviewCapsNoticeGivenOccupancyDaysAtTerminationEnd() {
    givenTermination(LocalDate.of(2026, 4, 15));
    LocalDate start = LocalDate.of(2026, 4, 1);
    LocalDate end = LocalDate.of(2026, 6, 30);
    Property property = new Property();
    property.setId(propertyId);
    when(paymentRepository.findByDateRange(start, end, TEAM_ID))
        .thenReturn(
            List.of(
                Payment.builder()
                    .id(UUID.randomUUID())
                    .contractId(underNotice.getId())
                    .amount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                    .status(Payment.PaymentStatus.PAID)
                    .build()));
    when(expenseRepository.findByDateRange(start, end, TEAM_ID)).thenReturn(List.of());
    when(propertyRepository.findByIdsAndTeamId(any(), eq(TEAM_ID))).thenReturn(List.of(property));

    FinancialOverviewResponse overview =
        service.getFinancialOverview(start, end, null, "EUR", principal);

    // April 1-15, not April 1 - June 30 (the contract's own end date).
    assertThat(overview.income().byProperty())
        .singleElement()
        .extracting(PropertyFinancialSummary::occupancyDays)
        .isEqualTo(15);
  }

  @Test
  @DisplayName(
      "date-range occupancy trend stops counting a NOTICE_GIVEN contract after the termination's "
          + "effective end date")
  void occupancyTrendByDateRangeStopsAtTerminationEnd() {
    givenTermination(LocalDate.of(2026, 4, 30));
    when(propertyRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of(new Property()));

    OccupancyTrendResponse trend =
        service.getOccupancyTrendByDateRange(
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), principal);

    assertThat(trend.dataPoints())
        .extracting(OccupancyTrendResponse.DataPoint::occupiedUnits)
        .containsExactly(1, 0, 0);
  }

  private void givenTermination(LocalDate effectiveEndDate) {
    when(contractTerminationRepository.findByContractIdsAndTeamId(any(), eq(TEAM_ID)))
        .thenReturn(
            Map.of(
                underNotice.getId(),
                ContractTermination.builder()
                    .id(UUID.randomUUID())
                    .teamId(TEAM_ID)
                    .contractId(underNotice.getId())
                    .computedEndDate(effectiveEndDate)
                    .effectiveEndDate(effectiveEndDate)
                    .status(ContractTerminationStatus.NOTICE_GIVEN)
                    .build()));
  }
}
