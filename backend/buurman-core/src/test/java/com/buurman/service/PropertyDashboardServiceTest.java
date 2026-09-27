package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.buurman.domain.Contract.ContractStatus;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.OccupancyDataPoint;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.FinancingPaymentRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyDashboardService")
class PropertyDashboardServiceTest {

  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private FinancingPaymentRepository financingPaymentRepository;
  @Mock private PropertyFinancialsService financialsService;
  @Mock private PropertyAcquisitionRepository acquisitionRepository;
  @Mock private PropertyOccupancyPeriodRepository occupancyPeriodRepository;
  @Mock private UnitRepository unitRepository;

  private PropertyDashboardService service;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneId.of("UTC"));
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Sid PROPERTY_IDENTIFIER = Sid.of("prop_test123456789012345");

  @BeforeEach
  void setUp() {
    service =
        new PropertyDashboardService(
            FIXED_CLOCK,
            propertyRepository,
            contractRepository,
            contractExtensionRepository,
            paymentRepository,
            expenseRepository,
            financingPaymentRepository,
            financialsService,
            acquisitionRepository,
            occupancyPeriodRepository,
            unitRepository);

    // Financial data and history lookups are irrelevant to occupancy and default to "no data"
    // for every test in this class; only unit/contract/occupancy-period stubs vary per test.
    lenient()
        .when(acquisitionRepository.findByPropertyIdAndTeamId(any(), any()))
        .thenReturn(Optional.empty());
    lenient()
        .when(financialsService.getLatestValuationAmount(any(), any()))
        .thenReturn(Optional.empty());
    lenient()
        .when(financialsService.sumActiveFinancingBalances(any(), any()))
        .thenReturn(Optional.empty());
    lenient()
        .when(financialsService.getTotalMonthlyFinancingPayment(any(), any()))
        .thenReturn(Optional.empty());
    lenient().when(financialsService.hasVariablePaymentFinancing(any(), any())).thenReturn(false);
    lenient()
        .when(financialsService.sumActiveAnnualTaxes(any(), any()))
        .thenReturn(Optional.empty());
    lenient()
        .when(financialsService.sumActiveAnnualInsurance(any(), any()))
        .thenReturn(Optional.empty());
    lenient()
        .when(financialsService.sumActiveAnnualFees(any(), any()))
        .thenReturn(Optional.empty());
    lenient().when(financialsService.getMonthlyOperatingCosts(any(), any())).thenReturn(Map.of());
    lenient()
        .when(paymentRepository.findPaidByContractIdsAndDateRange(any(), any(), any(), any()))
        .thenReturn(List.of());
    lenient()
        .when(paymentRepository.findPaidByContractIdsAndDueDateRange(any(), any(), any(), any()))
        .thenReturn(List.of());
    lenient().when(expenseRepository.findByPropertyId(any(), any())).thenReturn(List.of());
    lenient()
        .when(financingPaymentRepository.findByPropertyIdAndTeamId(any(), any()))
        .thenReturn(List.of());
    lenient()
        .when(contractExtensionRepository.findByContractIdsAndTeamId(any(), any()))
        .thenReturn(List.of());
  }

  private Property property(UUID propertyId) {
    Property property = new Property();
    property.setId(propertyId);
    property.setTeamId(TEAM_ID);
    property.setStreet("Keizersgracht 1");
    property.setCity("Amsterdam");
    property.setPostalCode("1015 CJ");
    return property;
  }

  private Contract activeContract(UUID propertyId, UUID unitId, LocalDate start, LocalDate end) {
    return Contract.builder()
        .id(UUID.randomUUID())
        .teamId(TEAM_ID)
        .propertyId(propertyId)
        .unitId(unitId)
        .startDate(start)
        .endDate(Optional.of(end))
        .status(ContractStatus.ACTIVE)
        .build();
  }

  @Test
  @DisplayName(
      "acceptance criterion: 4-unit building with 1 unit let for a full month reports 25% "
          + "occupancy, not 100%")
  void fourUnitBuildingWithOneUnitLetReportsTwentyFivePercent() {
    UUID propertyId = UUID.randomUUID();
    UUID unitA = UUID.randomUUID();
    Property property = property(propertyId);

    when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_IDENTIFIER, TEAM_ID))
        .thenReturn(property);
    // Unit A let for all of March; units B, C, D have no contracts and no occupancy history.
    Contract marchContract =
        activeContract(propertyId, unitA, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    when(contractRepository.findByPropertyId(propertyId, TEAM_ID))
        .thenReturn(List.of(marchContract));
    when(occupancyPeriodRepository.findByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of());
    when(unitRepository.countActiveByPropertyIdAndTeamId(propertyId, TEAM_ID)).thenReturn(4);

    PropertyDashboardResponse response =
        service.getDashboardData(
            PROPERTY_IDENTIFIER,
            1,
            TEAM_ID,
            Optional.of(LocalDate.of(2026, 3, 1)),
            Optional.of(LocalDate.of(2026, 3, 31)));

    assertThat(response.occupancy().months()).hasSize(1);
    OccupancyDataPoint march = response.occupancy().months().get(0);
    assertThat(march.tenantOccupancyPercent()).isEqualByComparingTo("25.00");
    assertThat(march.selfOccupancyPercent()).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("all 4 units let for the full month reports 100% occupancy")
  void fourUnitBuildingFullyLetReportsHundredPercent() {
    UUID propertyId = UUID.randomUUID();
    Property property = property(propertyId);

    when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_IDENTIFIER, TEAM_ID))
        .thenReturn(property);
    List<Contract> contracts =
        List.of(
            activeContract(
                propertyId, UUID.randomUUID(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
            activeContract(
                propertyId, UUID.randomUUID(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
            activeContract(
                propertyId, UUID.randomUUID(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
            activeContract(
                propertyId,
                UUID.randomUUID(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)));
    when(contractRepository.findByPropertyId(propertyId, TEAM_ID)).thenReturn(contracts);
    when(occupancyPeriodRepository.findByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of());
    when(unitRepository.countActiveByPropertyIdAndTeamId(propertyId, TEAM_ID)).thenReturn(4);

    PropertyDashboardResponse response =
        service.getDashboardData(
            PROPERTY_IDENTIFIER,
            1,
            TEAM_ID,
            Optional.of(LocalDate.of(2026, 3, 1)),
            Optional.of(LocalDate.of(2026, 3, 31)));

    OccupancyDataPoint march = response.occupancy().months().get(0);
    assertThat(march.tenantOccupancyPercent()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("no contracts and no occupancy history reports 0% occupancy")
  void noContractsReportsZeroPercent() {
    UUID propertyId = UUID.randomUUID();
    Property property = property(propertyId);

    when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_IDENTIFIER, TEAM_ID))
        .thenReturn(property);
    when(contractRepository.findByPropertyId(propertyId, TEAM_ID)).thenReturn(List.of());
    when(occupancyPeriodRepository.findByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of());
    when(unitRepository.countActiveByPropertyIdAndTeamId(propertyId, TEAM_ID)).thenReturn(4);

    PropertyDashboardResponse response =
        service.getDashboardData(
            PROPERTY_IDENTIFIER,
            1,
            TEAM_ID,
            Optional.of(LocalDate.of(2026, 3, 1)),
            Optional.of(LocalDate.of(2026, 3, 31)));

    OccupancyDataPoint march = response.occupancy().months().get(0);
    assertThat(march.tenantOccupancyPercent()).isEqualByComparingTo("0.00");
    assertThat(march.selfOccupancyPercent()).isEqualByComparingTo("0.00");
  }
}
