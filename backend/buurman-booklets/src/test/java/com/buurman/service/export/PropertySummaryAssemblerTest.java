package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.util.MoneyAmount;

@DisplayName("PropertySummaryAssembler")
@ExtendWith(MockitoExtension.class)
class PropertySummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final PropertyIdentifier ID = PropertyIdentifier.of("P-001");

  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private UnitResidentialDetailsRepository residentialDetailsRepository;
  @Mock private ContractRepository contractRepository;

  private PropertySummaryAssembler assembler;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/test-enum-labels");
    ms.setDefaultEncoding("UTF-8");
    ms.setUseCodeAsDefaultMessage(true);

    assembler =
        new PropertySummaryAssembler(
            propertyRepository,
            unitRepository,
            residentialDetailsRepository,
            contractRepository,
            new BookletFormatter(),
            new EnumLabelResolver(ms),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");
  }

  @Test
  @DisplayName("single-unit property reads its lone unit's status/area/energy directly")
  void assemblesSingleUnitProperty() {
    UUID propertyId = UUID.randomUUID();
    Property property = Property.builder().id(propertyId).street("Kerkstraat 14").build();
    Unit unit = unit(propertyId, "1", UnitStatus.OCCUPIED, new BigDecimal("85.50"), "A");
    Contract contract =
        Contract.builder()
            .id(UUID.randomUUID())
            .startDate(LocalDate.parse("2024-01-01"))
            .rentAmount(MoneyAmount.of(new BigDecimal("1450.00"), "EUR"))
            .build();

    when(propertyRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(property);
    when(unitRepository.findAllByPropertyIdAndTeamId(propertyId, TEAM)).thenReturn(List.of(unit));
    when(residentialDetailsRepository.findByUnitIdAndTeamId(unit.getId(), TEAM))
        .thenReturn(Optional.of(residential(3, 2)));
    when(contractRepository.findActiveByUnitId(unit.getId(), TEAM))
        .thenReturn(Optional.of(contract));

    Map<String, Object> v = assembler.assemble(ID, TEAM, java.util.Locale.ENGLISH);

    assertThat(v.get("statusCode")).isEqualTo("OCCUPIED");
    assertThat(v.get("area")).isEqualTo("85.5");
    assertThat(v.get("energyLabel")).isEqualTo("A");
    assertThat(v.get("bedBath")).isEqualTo("3 / 2");
    assertThat(v.get("headlineMoney")).asString().contains("1,450");
    assertThat(v.get("isVacant")).isEqualTo(false);
  }

  @Test
  @DisplayName(
      "multi-unit property shows an occupancy fraction and a summed area instead of one unit's"
          + " value")
  void assemblesMultiUnitProperty() {
    UUID propertyId = UUID.randomUUID();
    Property property = Property.builder().id(propertyId).street("Rozengracht 200").build();
    Unit occupiedUnit = unit(propertyId, "1", UnitStatus.OCCUPIED, new BigDecimal("40.00"), "B");
    Unit vacantUnit = unit(propertyId, "2", UnitStatus.VACANT, new BigDecimal("35.00"), "C");

    when(propertyRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(property);
    when(unitRepository.findAllByPropertyIdAndTeamId(propertyId, TEAM))
        .thenReturn(List.of(occupiedUnit, vacantUnit));
    when(residentialDetailsRepository.findByUnitIdAndTeamId(any(), any()))
        .thenReturn(Optional.empty());
    when(contractRepository.findActiveByUnitId(any(), any())).thenReturn(Optional.empty());

    Map<String, Object> v = assembler.assemble(ID, TEAM, java.util.Locale.ENGLISH);

    assertThat(v.get("statusCode")).isEqualTo("MIXED");
    assertThat(v.get("statusLabel")).isEqualTo("1/2 Occupied");
    assertThat(v.get("area")).isEqualTo("75");
    // A letter grade cannot be averaged: two different ratings collapse to "—", never one unit's
    // value silently standing in for the whole property.
    assertThat(v.get("energyLabel")).isEqualTo("—");
    assertThat(v.get("headlineMoney")).isEqualTo("—");
    assertThat(v).doesNotContainKey("tenure");
  }

  private Unit unit(
      UUID propertyId, String unitNumber, UnitStatus status, BigDecimal area, String energyLabel) {
    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setTeamId(TEAM);
    unit.setPropertyId(propertyId);
    unit.setUnitNumber(unitNumber);
    unit.setUnitType(UnitType.APARTMENT);
    unit.setStatus(status);
    unit.setAreaValue(Optional.of(area));
    unit.setAreaUnit(Optional.of("sqm"));
    unit.setEnergyEfficiencyRating(Optional.of(energyLabel));
    return unit;
  }

  private UnitResidentialDetails residential(int bedrooms, int bathrooms) {
    return UnitResidentialDetails.builder()
        .bedrooms(Optional.of(bedrooms))
        .bathrooms(Optional.of(bathrooms))
        .build();
  }
}
