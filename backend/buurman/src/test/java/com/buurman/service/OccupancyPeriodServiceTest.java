package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyStatus;
import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("OccupancyPeriodService")
class OccupancyPeriodServiceTest {

  @Mock private PropertyOccupancyPeriodRepository repository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PropertyAcquisitionRepository acquisitionRepository;
  @Mock private PropertyFinancingRepository financingRepository;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private OccupancyPeriodService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final PropertyIdentifier PROPERTY_SID =
      PropertyIdentifier.of("prop_01JTEST000000000000000001");

  private UserPrincipal principal;
  private Property property;

  @BeforeEach
  void setUp() {
    service =
        new OccupancyPeriodService(
            repository,
            propertyRepository,
            contractRepository,
            contractExtensionRepository,
            acquisitionRepository,
            financingRepository,
            clock);

    principal =
        new UserPrincipal(
            USER_ID, "usr_test", "kc-id", "test@example.com", "Test User",
            TEAM_ID, "team_test", TeamRole.TEAM_ADMIN, true);

    property = new Property();
    property.setId(PROPERTY_ID);
    property.setIdentifier(Optional.of(PROPERTY_SID));
    property.setStatus(PropertyStatus.VACANT);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("creates period when no overlaps exist")
    void createsWhenNoOverlaps() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID))
          .thenReturn(property);
      when(repository.findOverlapping(
              eq(PROPERTY_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 1),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 6, 30)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      OccupancyPeriodResponse response = service.create(PROPERTY_SID, request, principal);

      assertThat(response).isNotNull();
      assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
      verify(repository).save(any(PropertyOccupancyPeriod.class));
    }

    @Test
    @DisplayName("rejects overlapping occupancy periods")
    void rejectsOverlapping() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID))
          .thenReturn(property);

      PropertyOccupancyPeriod existing = new PropertyOccupancyPeriod();
      existing.setId(UUID.randomUUID());
      when(repository.findOverlapping(
              eq(PROPERTY_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of(existing));

      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 4, 1),
              OccupancyType.PERSONAL,
              Optional.of(LocalDate.of(2026, 6, 30)),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      assertThatThrownBy(() -> service.create(PROPERTY_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("overlaps with an existing period");
    }

    @Test
    @DisplayName("sets property to SELF_OCCUPIED when period starts today or earlier")
    void setsPropertyStatusWhenStartsToday() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID))
          .thenReturn(property);
      when(repository.findOverlapping(
              eq(PROPERTY_ID), eq(TEAM_ID), any(LocalDate.class), any(LocalDate.class), isNull()))
          .thenReturn(List.of());
      when(contractRepository.findByPropertyId(PROPERTY_ID, TEAM_ID)).thenReturn(List.of());
      when(contractExtensionRepository.findByContractIdsAndTeamId(List.of(), TEAM_ID))
          .thenReturn(List.of());

      // Start date is today (2026-03-01, matching our fixed clock)
      CreateOccupancyPeriodRequest request =
          new CreateOccupancyPeriodRequest(
              LocalDate.of(2026, 3, 1),
              OccupancyType.PERSONAL,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      service.create(PROPERTY_SID, request, principal);

      assertThat(property.getStatus()).isEqualTo(PropertyStatus.SELF_OCCUPIED);
      verify(propertyRepository).save(property);
    }
  }
}
