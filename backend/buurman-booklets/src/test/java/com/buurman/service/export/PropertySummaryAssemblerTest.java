package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
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
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;
import com.buurman.util.MoneyAmount;

@DisplayName("PropertySummaryAssembler")
@ExtendWith(MockitoExtension.class)
class PropertySummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final PropertyIdentifier ID = PropertyIdentifier.of("P-001");

  @Mock private PropertyRepository propertyRepository;
  @Mock private PropertyResidentialDetailsRepository residentialDetailsRepository;
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
            residentialDetailsRepository,
            contractRepository,
            new BookletFormatter(),
            new EnumLabelResolver(ms),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");
  }

  @Test
  @DisplayName("projects an occupied property with active tenancy into the card variables")
  void assemblesOccupied() {
    UUID propertyId = UUID.randomUUID();
    Property property =
        Property.builder()
            .id(propertyId)
            .street("Kerkstraat 14")
            .city("Amsterdam")
            .postalCode("1017 GC")
            .propertyType(Property.PropertyType.APARTMENT)
            .propertyCategory(Property.PropertyCategory.RESIDENTIAL)
            .status(Property.PropertyStatus.OCCUPIED)
            .areaValue(Optional.of(new BigDecimal("84")))
            .areaUnit(Optional.of("m²"))
            .yearBuilt(Optional.of(2019))
            .energyEfficiencyRating(Optional.of("A"))
            .build();
    PropertyResidentialDetails residential =
        PropertyResidentialDetails.builder()
            .bedrooms(Optional.of(2))
            .bathrooms(Optional.of(1))
            .build();
    Contract active =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(propertyId)
            .status(Contract.ContractStatus.ACTIVE)
            .startDate(LocalDate.parse("2023-01-01"))
            .endDate(Optional.of(LocalDate.parse("2026-12-31")))
            .rentAmount(MoneyAmount.of(new BigDecimal("1450.00"), "EUR"))
            .build();

    when(propertyRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(property);
    when(residentialDetailsRepository.findByPropertyIdAndTeamId(propertyId, TEAM))
        .thenReturn(Optional.of(residential));
    when(contractRepository.findByPropertyId(propertyId, TEAM)).thenReturn(List.of(active));

    Map<String, Object> v = assembler.assemble(ID, TEAM, Locale.ENGLISH);

    assertThat(v.get("propertyIdentifier")).isEqualTo("P-001");
    assertThat(v.get("propertyAddress")).isEqualTo("Kerkstraat 14");
    assertThat(v.get("propertyTypeLabel")).isEqualTo("Apartment");
    assertThat(v.get("propertyCategoryLabel")).isEqualTo("Residential");
    assertThat(v.get("statusCode")).isEqualTo("OCCUPIED");
    assertThat(v.get("heroStatusWord")).isEqualTo("OCCUPIED");
    assertThat(v.get("heroGroundClass")).isEqualTo("hs-occupied");
    assertThat(v.get("area")).isEqualTo("84");
    assertThat(v.get("bedBath")).isEqualTo("2 / 1");
    assertThat(v.get("energyLabel")).isEqualTo("A");
    assertThat(v.get("yearBuilt")).isEqualTo("2019");
    assertThat(v.get("isVacant")).isEqualTo(false);
    assertThat((String) v.get("headlineMoney")).contains("1,450");
    assertThat(v).containsKey("tenure");
    assertThat((String) v.get("qrDataUri")).startsWith("data:image/svg+xml;base64,");
  }

  @Test
  @DisplayName("flags a property with no active contract as vacant")
  void assemblesVacant() {
    UUID propertyId = UUID.randomUUID();
    Property property =
        Property.builder()
            .id(propertyId)
            .street("Lege Laan 1")
            .propertyType(Property.PropertyType.STUDIO)
            .propertyCategory(Property.PropertyCategory.RESIDENTIAL)
            .status(Property.PropertyStatus.VACANT)
            .build();
    when(propertyRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(property);
    when(residentialDetailsRepository.findByPropertyIdAndTeamId(propertyId, TEAM))
        .thenReturn(Optional.empty());
    when(contractRepository.findByPropertyId(propertyId, TEAM)).thenReturn(List.of());

    Map<String, Object> v = assembler.assemble(ID, TEAM, Locale.ENGLISH);

    assertThat(v.get("isVacant")).isEqualTo(true);
    assertThat(v.get("heroGroundClass")).isEqualTo("hs-vacant");
    assertThat(v.get("headlineMoney")).isEqualTo("—");
    assertThat(v.get("bedBath")).isEqualTo("—");
  }
}
