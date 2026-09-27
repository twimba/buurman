package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;

import com.buurman.document.DocumentRenderer;
import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyType;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyAgriculturalDetailsRepository;
import com.buurman.repository.PropertyCommercialDetailsRepository;
import com.buurman.repository.PropertyIndustrialDetailsRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UnitAmenityRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;
import com.buurman.service.S3StorageService;

/**
 * End-to-end (no Gotenberg) coverage of the property dossier: repositories are mocked, but the real
 * production {@code document-property-booklet}/{@code document-enum-labels} bundles and the real
 * Thymeleaf template are used, so a missing message key or a template binding mistake fails here
 * rather than only being caught by a manual render. {@link DocumentRenderer} is mocked to hand back
 * the HTML unmodified so it can be asserted on directly.
 */
@DisplayName("PropertyBookletExporter")
@ExtendWith(MockitoExtension.class)
class PropertyBookletExporterTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final PropertyIdentifier PROPERTY_ID = PropertyIdentifier.of("P-0001");

  @Mock private PropertyRepository propertyRepository;
  @Mock private PropertyCommercialDetailsRepository commercialDetailsRepository;
  @Mock private PropertyIndustrialDetailsRepository industrialDetailsRepository;
  @Mock private PropertyAgriculturalDetailsRepository agriculturalDetailsRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private PropertyOutdoorAreaRepository propertyOutdoorAreaRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private UnitResidentialDetailsRepository unitResidentialDetailsRepository;
  @Mock private UnitAmenityRepository unitAmenityRepository;
  @Mock private PhotoRepository photoRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private S3StorageService s3StorageService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private PropertyDashboardService propertyDashboardService;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private DocumentRenderer pdfRenderer;

  private PropertyBookletExporter exporter;
  private UUID propertyId;

  @BeforeEach
  void setUp() {
    MessageSource messageSource =
        DocumentTemplateSupport.messageSource(
            false,
            "classpath:messages/document-property-booklet",
            "classpath:messages/document-enum-labels",
            "classpath:messages/document-summary-card");
    TemplateEngine templateEngine = DocumentTemplateSupport.templateEngine(messageSource, false);

    exporter =
        new PropertyBookletExporter(
            propertyRepository,
            commercialDetailsRepository,
            industrialDetailsRepository,
            agriculturalDetailsRepository,
            contractRepository,
            contractExtensionRepository,
            paymentRepository,
            expenseRepository,
            propertyOutdoorAreaRepository,
            unitRepository,
            unitResidentialDetailsRepository,
            unitAmenityRepository,
            photoRepository,
            contractPartyService,
            s3StorageService,
            featureFlagService,
            propertyDashboardService,
            teamPreferencesRepository,
            pdfRenderer,
            templateEngine,
            messageSource,
            new EnumLabelResolver(messageSource),
            new BookletFormatter(),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");

    propertyId = UUID.randomUUID();
    when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_ID, TEAM_ID))
        .thenReturn(
            Property.builder()
                .id(propertyId)
                .street("Kerkstraat 14")
                .city("Amsterdam")
                .propertyCategory(PropertyCategory.RESIDENTIAL)
                .propertyType(PropertyType.APARTMENT)
                .build());
    when(contractRepository.findByPropertyId(propertyId, TEAM_ID)).thenReturn(List.of());
    when(paymentRepository.findAllByTeamId(TEAM_ID)).thenReturn(List.of());
    when(expenseRepository.findByPropertyId(propertyId, TEAM_ID)).thenReturn(List.of());
    when(propertyOutdoorAreaRepository.findByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of());
    when(photoRepository.findByEntityAndTeamId("PROPERTY", propertyId, TEAM_ID))
        .thenReturn(List.of());
    when(teamPreferencesRepository.getByTeamId(TEAM_ID))
        .thenReturn(TeamPreferences.builder().build());
    when(featureFlagService.isEnabled(any())).thenReturn(false);
    lenient()
        .when(unitAmenityRepository.findAmenitiesByUnitIdAndTeamId(any(), any()))
        .thenReturn(List.of());
    lenient()
        .when(contractRepository.findActiveByUnitId(any(), any()))
        .thenReturn(Optional.empty());
    when(pdfRenderer.render(anyString(), any()))
        .thenAnswer(inv -> ((String) inv.getArgument(0)).getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("single-unit property reads dwelling fields directly off its lone unit")
  void singleUnitReadsDwellingFieldsDirectly() {
    Unit unit =
        Unit.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .propertyId(propertyId)
            .unitNumber("1")
            .unitType(UnitType.APARTMENT)
            .status(UnitStatus.OCCUPIED)
            .areaValue(Optional.of(new BigDecimal("85.50")))
            .areaUnit(Optional.of("sqm"))
            .energyEfficiencyRating(Optional.of("A"))
            .heatingType(Optional.of("DISTRICT_HEATING"))
            .insulationNotes(Optional.of("Triple glazing throughout."))
            .hasSmokeDetectors(Optional.of(true))
            .hasAdaptedBathroom(Optional.of(true))
            .accessibilityNotes(Optional.of("Ramp at entrance."))
            .build();
    when(unitRepository.findAllByPropertyIdAndTeamId(propertyId, TEAM_ID))
        .thenReturn(List.of(unit));
    when(unitResidentialDetailsRepository.findByUnitIdAndTeamId(unit.getId(), TEAM_ID))
        .thenReturn(
            Optional.of(
                UnitResidentialDetails.builder()
                    .id(UUID.randomUUID())
                    .unitId(unit.getId())
                    .teamId(TEAM_ID)
                    .bedrooms(Optional.of(3))
                    .bathrooms(Optional.of(2))
                    .furnished(true)
                    .build()));

    String html =
        new String(exporter.generate(PROPERTY_ID, TEAM_ID, Locale.ENGLISH), StandardCharsets.UTF_8);

    // No Units page for a single-unit property.
    assertThat(html).doesNotContain(">Units<");
    // Status/area/energy/insulation/safety/accessibility all sourced from the lone unit.
    assertThat(html)
        .contains("Occupied")
        .contains("85.5")
        .contains("A")
        .contains("District Heating");
    assertThat(html).contains("Triple glazing throughout.").contains("Smoke Detectors");
    assertThat(html).contains("Ramp at entrance.");
    assertThat(html).contains("3").contains("Bedrooms");
  }

  @Test
  @DisplayName(
      "caps verbose per-unit detail blocks at 20 for a 50-unit building but lists every unit")
  void capsDetailBlocksForLargeBuilding() {
    List<Unit> units = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      units.add(
          Unit.builder()
              .id(UUID.randomUUID())
              .teamId(TEAM_ID)
              .propertyId(propertyId)
              .unitNumber(String.valueOf(i))
              .sortOrder(i)
              .unitType(UnitType.APARTMENT)
              .status(i % 3 == 0 ? UnitStatus.VACANT : UnitStatus.OCCUPIED)
              .areaValue(Optional.of(new BigDecimal("50")))
              .build());
    }
    when(unitRepository.findAllByPropertyIdAndTeamId(propertyId, TEAM_ID)).thenReturn(units);
    when(unitResidentialDetailsRepository.findByUnitIdAndTeamId(any(), any()))
        .thenReturn(Optional.empty());

    String html =
        new String(exporter.generate(PROPERTY_ID, TEAM_ID, Locale.ENGLISH), StandardCharsets.UTF_8);

    // Every unit is in the table (one "num" cell per row), but only 20 verbose detail cards.
    assertThat(countOccurrences(html, "<td class=\"num\">")).isEqualTo(50);
    assertThat(countOccurrences(html, "class=\"unitcard\"")).isEqualTo(20);
    assertThat(html).contains("and 30 more units");
    // Aggregate area (50 units × 50 sqm) and occupancy fraction.
    assertThat(html).contains("2500");
    assertThat(html).contains("Occupied");
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int index = 0;
    while ((index = haystack.indexOf(needle, index)) != -1) {
      count++;
      index += needle.length();
    }
    return count;
  }
}
