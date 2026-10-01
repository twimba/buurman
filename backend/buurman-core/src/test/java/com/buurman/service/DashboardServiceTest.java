package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository extensionRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private TeamService teamService;

  private DashboardService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new DashboardService(
            auditLogRepository,
            propertyRepository,
            unitRepository,
            contractRepository,
            extensionRepository,
            contactRepository,
            contractPartyService,
            teamService,
            FIXED_CLOCK);
  }

  private Unit unit(UnitStatus status) {
    return Unit.builder()
        .id(UUID.randomUUID())
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .unitNumber("1")
        .unitType(UnitType.APARTMENT)
        .status(status)
        .build();
  }

  @Test
  @DisplayName(
      "totalUnits reconciles with the per-status counts: a 6-unit building with 3 occupied and 3 "
          + "vacant reports totalUnits=6, not just totalProperties=1")
  void totalUnitsReconcilesWithStatusCounts() {
    when(propertyRepository.countByTeamId(TEAM_ID)).thenReturn(1);
    when(unitRepository.findAllByTeamId(TEAM_ID))
        .thenReturn(
            List.of(
                unit(UnitStatus.OCCUPIED),
                unit(UnitStatus.OCCUPIED),
                unit(UnitStatus.OCCUPIED),
                unit(UnitStatus.VACANT),
                unit(UnitStatus.VACANT),
                unit(UnitStatus.VACANT)));
    when(contractRepository.findInForceContractIncomeByTeamId(TEAM_ID)).thenReturn(List.of());
    when(teamService.getDefaultCurrency(TEAM_ID)).thenReturn("EUR");

    DashboardStatsResponse response = service.getDashboardStats(TEAM_ID);

    assertThat(response.totalProperties()).isEqualTo(1);
    assertThat(response.totalUnits()).isEqualTo(6);
    assertThat(response.occupiedUnits()).isEqualTo(3);
    assertThat(response.vacantUnits()).isEqualTo(3);
    assertThat(response.occupiedUnits() + response.vacantUnits()).isEqualTo(response.totalUnits());
  }

  @Test
  @DisplayName("totalUnits is 0 for a team with no units")
  void totalUnitsIsZeroWhenNoUnits() {
    when(propertyRepository.countByTeamId(TEAM_ID)).thenReturn(0);
    when(unitRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of());
    when(contractRepository.findInForceContractIncomeByTeamId(TEAM_ID)).thenReturn(List.of());
    when(teamService.getDefaultCurrency(TEAM_ID)).thenReturn("EUR");

    DashboardStatsResponse response = service.getDashboardStats(TEAM_ID);

    assertThat(response.totalUnits()).isZero();
  }
}
