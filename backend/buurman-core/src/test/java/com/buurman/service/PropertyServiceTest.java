package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyType;
import com.buurman.domain.SortDirection;
import com.buurman.domain.TeamRole;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.ResidentialDetailsRequest;
import com.buurman.dto.request.UpdateAllocationRequest;
import com.buurman.dto.request.UpdateAllocationRequest.UnitShareEntry;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.UnitSummaryResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.OptionalMappingConfig;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.PropertyMapperImpl;
import com.buurman.mapper.UnitMapper;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyAgriculturalDetailsRepository;
import com.buurman.repository.PropertyCommercialDetailsRepository;
import com.buurman.repository.PropertyIndustrialDetailsRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitAmenityRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.util.SidGenerator;

/**
 * Uses the real (MapStruct-generated) {@link PropertyMapperImpl} rather than mocking {@link
 * PropertyMapper}: {@link PropertyResponse} carries ~40 fields, and hand-building a mock fixture
 * for every field would obscure what each test actually checks. {@link UnitMapper} stays mocked —
 * its one call site here ({@code toSummary}) is trivial to stub directly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyService")
class PropertyServiceTest {

  @Mock private PropertyRepository propertyRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private UnitAmenityRepository unitAmenityRepository;
  @Mock private UnitResidentialDetailsRepository unitResidentialDetailsRepository;
  @Mock private UnitService unitService;
  @Mock private UnitMapper unitMapper;
  @Mock private PropertyCommercialDetailsRepository commercialDetailsRepository;
  @Mock private PropertyIndustrialDetailsRepository industrialDetailsRepository;
  @Mock private PropertyAgriculturalDetailsRepository agriculturalDetailsRepository;
  @Mock private AuditService auditService;
  @Mock private DocumentService documentService;
  @Mock private PhotoService photoService;
  @Mock private PhotoRepository photoRepository;
  @Mock private S3StorageService s3StorageService;
  @Mock private PropertyOutdoorAreaRepository outdoorAreaRepository;
  @Mock private MetricsService metricsService;
  @Mock private NotificationService notificationService;
  @Mock private AppProperties appProperties;
  @Mock private GeocodingService geocodingService;

  private PropertyMapper propertyMapper;
  private PropertyService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final PropertyIdentifier PROPERTY_SID =
      PropertyIdentifier.of("prop_01JTEST000000000000000001");

  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    propertyMapper = wireRealPropertyMapper();

    service =
        new PropertyService(
            propertyRepository,
            unitRepository,
            unitAmenityRepository,
            unitResidentialDetailsRepository,
            unitService,
            unitMapper,
            commercialDetailsRepository,
            industrialDetailsRepository,
            agriculturalDetailsRepository,
            propertyMapper,
            auditService,
            documentService,
            photoService,
            photoRepository,
            s3StorageService,
            outdoorAreaRepository,
            metricsService,
            notificationService,
            appProperties,
            geocodingService);

    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN);

    // Only createProperty sends a notification (which reads appProperties.email()); lenient so
    // the getProperty tests below don't trip strict-stubbing.
    org.mockito.Mockito.lenient()
        .when(appProperties.email())
        .thenReturn(new AppProperties.Email("no-reply@buurman.io", "Buurman", "https://app.test"));
  }

  private static PropertyMapper wireRealPropertyMapper() {
    PropertyMapperImpl impl = new PropertyMapperImpl();
    try {
      Field field = PropertyMapperImpl.class.getDeclaredField("optionalMappingConfig");
      field.setAccessible(true);
      field.set(impl, new OptionalMappingConfig());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to wire OptionalMappingConfig into PropertyMapperImpl", e);
    }
    return impl;
  }

  private Property savedProperty(PropertyCategory category, PropertyType type) {
    return Property.builder()
        .id(PROPERTY_ID)
        .identifier(Optional.of(PROPERTY_SID))
        .teamId(TEAM_ID)
        .street("Keizersgracht 1")
        .city("Amsterdam")
        .postalCode("1015CJ")
        .countryCode("NL")
        .propertyCategory(category)
        .propertyType(type)
        .allocationBasis(AllocationBasis.EQUAL)
        .createdAt(Instant.parse("2026-03-01T12:00:00Z"))
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  private CreatePropertyRequest request(
      PropertyCategory category, PropertyType type, @Nullable CreateUnitRequest unit) {
    return request(category, type, unit, Optional.empty());
  }

  private CreatePropertyRequest request(
      PropertyCategory category,
      PropertyType type,
      @Nullable CreateUnitRequest unit,
      Optional<ResidentialDetailsRequest> residentialDetails) {
    return new CreatePropertyRequest(
        category,
        type,
        "Keizersgracht 1",
        "Amsterdam",
        "1015CJ",
        "NL",
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        residentialDetails,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        unit);
  }

  private CreateUnitRequest unitRequest(
      String unitNumber, UnitType unitType, UnitStatus status, BigDecimal allocationShare) {
    return new CreateUnitRequest(
        unitNumber,
        Optional.empty(),
        Optional.empty(),
        unitType,
        Optional.of(status),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(allocationShare),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private Unit unit(String unitNumber, UnitType type, UnitStatus status) {
    return Unit.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(SidGenerator.newUnitId()))
        .teamId(TEAM_ID)
        .propertyId(PROPERTY_ID)
        .unitNumber(unitNumber)
        .unitType(type)
        .status(status)
        .build();
  }

  private UpdatePropertyRequest updateRequest(
      Optional<ResidentialDetailsRequest> residentialDetails) {
    return new UpdatePropertyRequest(
        PropertyType.APARTMENT,
        "Keizersgracht 1",
        "Amsterdam",
        "1015CJ",
        "NL",
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        residentialDetails,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @Nested
  @DisplayName("createProperty")
  class CreateProperty {

    @Test
    @DisplayName("with a unit payload creates exactly one unit carrying those values, not implicit")
    void createsSuppliedUnitNotImplicit() {
      CreateUnitRequest suppliedUnit =
          unitRequest("3B", UnitType.APARTMENT, UnitStatus.OCCUPIED, new BigDecimal("40"));
      CreatePropertyRequest req =
          request(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT, suppliedUnit);

      when(propertyRepository.save(any(Property.class)))
          .thenReturn(savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT));

      service.createProperty(req, principal);

      ArgumentCaptor<CreateUnitRequest> unitCaptor =
          ArgumentCaptor.forClass(CreateUnitRequest.class);
      ArgumentCaptor<Boolean> implicitCaptor = ArgumentCaptor.forClass(Boolean.class);
      verify(unitService)
          .createInitialUnit(
              eq(PROPERTY_ID), unitCaptor.capture(), implicitCaptor.capture(), eq(principal));

      assertThat(unitCaptor.getValue()).isSameAs(suppliedUnit);
      assertThat(unitCaptor.getValue().unitNumber()).isEqualTo("3B");
      assertThat(unitCaptor.getValue().allocationShare()).contains(new BigDecimal("40"));
      assertThat(implicitCaptor.getValue()).isFalse();
    }

    @Test
    @DisplayName(
        "with no unit creates exactly one implicit VACANT unit numbered \"1\" with 100% share,"
            + " APARTMENT for a RESIDENTIAL property")
    void createsImplicitUnitForResidential() {
      CreatePropertyRequest req =
          request(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT, null);

      when(propertyRepository.save(any(Property.class)))
          .thenReturn(savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT));

      service.createProperty(req, principal);

      ArgumentCaptor<CreateUnitRequest> unitCaptor =
          ArgumentCaptor.forClass(CreateUnitRequest.class);
      ArgumentCaptor<Boolean> implicitCaptor = ArgumentCaptor.forClass(Boolean.class);
      verify(unitService)
          .createInitialUnit(
              eq(PROPERTY_ID), unitCaptor.capture(), implicitCaptor.capture(), eq(principal));

      CreateUnitRequest implicit = unitCaptor.getValue();
      assertThat(implicit.unitNumber()).isEqualTo("1");
      assertThat(implicit.status()).contains(UnitStatus.VACANT);
      assertThat(implicit.allocationShare()).contains(new BigDecimal("100"));
      assertThat(implicit.unitType()).isEqualTo(UnitType.APARTMENT);
      assertThat(implicitCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("with no unit derives COMMERCIAL unit type for a COMMERCIAL property")
    void createsImplicitUnitForCommercial() {
      CreatePropertyRequest req = request(PropertyCategory.COMMERCIAL, PropertyType.OFFICE, null);

      when(propertyRepository.save(any(Property.class)))
          .thenReturn(savedProperty(PropertyCategory.COMMERCIAL, PropertyType.OFFICE));

      service.createProperty(req, principal);

      ArgumentCaptor<CreateUnitRequest> unitCaptor =
          ArgumentCaptor.forClass(CreateUnitRequest.class);
      verify(unitService)
          .createInitialUnit(eq(PROPERTY_ID), unitCaptor.capture(), eq(true), eq(principal));

      assertThat(unitCaptor.getValue().unitType()).isEqualTo(UnitType.COMMERCIAL);
    }

    @Test
    @DisplayName(
        "with RESIDENTIAL category and residentialDetails persists them onto the newly created"
            + " implicit unit (BUUR-106)")
    void persistsResidentialDetailsOntoImplicitUnit() {
      ResidentialDetailsRequest residential =
          new ResidentialDetailsRequest(
              Optional.of(3), Optional.of(2), Optional.of(true), Optional.of("cats-only"));
      CreatePropertyRequest req =
          request(
              PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT, null, Optional.of(residential));

      when(propertyRepository.save(any(Property.class)))
          .thenReturn(savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT));

      Unit implicitUnit = unit("1", UnitType.APARTMENT, UnitStatus.VACANT);
      when(unitService.createInitialUnit(eq(PROPERTY_ID), any(), eq(true), eq(principal)))
          .thenReturn(implicitUnit);
      when(unitResidentialDetailsRepository.findByUnitIdAndTeamId(implicitUnit.getId(), TEAM_ID))
          .thenReturn(Optional.empty());

      service.createProperty(req, principal);

      ArgumentCaptor<UnitResidentialDetails> captor =
          ArgumentCaptor.forClass(UnitResidentialDetails.class);
      verify(unitResidentialDetailsRepository).save(captor.capture());

      UnitResidentialDetails saved = captor.getValue();
      assertThat(saved.getUnitId()).isEqualTo(implicitUnit.getId());
      assertThat(saved.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(saved.getBedrooms()).contains(3);
      assertThat(saved.getBathrooms()).contains(2);
      assertThat(saved.isFurnished()).isTrue();
      assertThat(saved.getPetPolicy()).contains("cats-only");
    }
  }

  @Nested
  @DisplayName("getProperty")
  class GetProperty {

    @Test
    @DisplayName(
        "with 4 units, 1 occupied, returns unitCount 4, occupiedUnitCount 1, vacantUnitCount 3")
    void returnsUnitFacts() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);

      List<Unit> units =
          List.of(
              unit("1", UnitType.APARTMENT, UnitStatus.OCCUPIED),
              unit("2", UnitType.APARTMENT, UnitStatus.VACANT),
              unit("3", UnitType.APARTMENT, UnitStatus.VACANT),
              unit("4", UnitType.APARTMENT, UnitStatus.VACANT));
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(units);
      when(unitMapper.toSummary(any(Unit.class)))
          .thenAnswer(
              inv -> {
                Unit u = inv.getArgument(0);
                return new UnitSummaryResponse(
                    u.getIdentifier().orElseThrow(),
                    u.getUnitNumber(),
                    u.getName(),
                    u.getUnitType(),
                    u.getStatus());
              });

      PropertyResponse response = service.getProperty(PROPERTY_SID, principal);

      assertThat(response.unitCount()).isEqualTo(4);
      assertThat(response.occupiedUnitCount()).isEqualTo(1);
      assertThat(response.vacantUnitCount()).isEqualTo(3);
      assertThat(response.units()).hasSize(4);
    }

    @Test
    @DisplayName(
        "unitCount/occupiedUnitCount/vacantUnitCount agree with the paginated list for a property"
            + " with 1 OCCUPIED, 1 VACANT, 2 MAINTENANCE units")
    void unitFactsAgreeBetweenDetailAndListEndpoints() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);

      // Detail path: getProperty derives counts from the full unit list.
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      List<Unit> units =
          List.of(
              unit("1", UnitType.APARTMENT, UnitStatus.OCCUPIED),
              unit("2", UnitType.APARTMENT, UnitStatus.VACANT),
              unit("3", UnitType.APARTMENT, UnitStatus.MAINTENANCE),
              unit("4", UnitType.APARTMENT, UnitStatus.MAINTENANCE));
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(units);
      when(unitMapper.toSummary(any(Unit.class)))
          .thenAnswer(
              inv -> {
                Unit u = inv.getArgument(0);
                return new UnitSummaryResponse(
                    u.getIdentifier().orElseThrow(),
                    u.getUnitNumber(),
                    u.getName(),
                    u.getUnitType(),
                    u.getStatus());
              });

      PropertyResponse detail = service.getProperty(PROPERTY_SID, principal);

      // List path: getPropertiesPaginated derives counts from the precomputed UnitCounts map,
      // which must carry the same 1/1 split (not "total - occupied", which would report 3).
      when(propertyRepository.findAllByTeamIdPaginated(eq(TEAM_ID), any(), any(), any(), any()))
          .thenReturn(new PaginatedResult<>(List.of(property), 1));
      when(propertyRepository.findUnitCountsByTeamId(TEAM_ID))
          .thenReturn(Map.of(PROPERTY_ID, new PropertyRepository.UnitCounts(4, 1, 1)));

      PageResponse<PropertyResponse> page =
          service.getPropertiesPaginated(
              principal, null, null, null, PageRequest.of(null, null, null, (SortDirection) null));
      PropertyResponse listItem = page.content().get(0);

      assertThat(detail.unitCount()).isEqualTo(4);
      assertThat(detail.occupiedUnitCount()).isEqualTo(1);
      assertThat(detail.vacantUnitCount()).isEqualTo(1);

      assertThat(listItem.unitCount()).isEqualTo(detail.unitCount());
      assertThat(listItem.occupiedUnitCount()).isEqualTo(detail.occupiedUnitCount());
      assertThat(listItem.vacantUnitCount()).isEqualTo(detail.vacantUnitCount());
    }
  }

  @Nested
  @DisplayName("updateAllocation (BUUR-106 Important 3 & 5)")
  class UpdateAllocation {

    @Test
    @DisplayName("rejects MANUAL as a property-level basis — it is a per-expense override")
    void rejectsManualBasis() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);

      UpdateAllocationRequest request =
          new UpdateAllocationRequest(AllocationBasis.MANUAL, Optional.empty());

      assertThatThrownBy(() -> service.updateAllocation(PROPERTY_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("per-expense override");

      verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUSTOM rejects a request missing a share for one of three active units")
    void rejectsCustomMissingUnit() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);

      UnitIdentifier unit1Id = SidGenerator.newUnitId();
      UnitIdentifier unit2Id = SidGenerator.newUnitId();
      Unit unit1 = unitWithIdentifier(unit1Id, "1", UnitStatus.OCCUPIED);
      Unit unit2 = unitWithIdentifier(unit2Id, "2", UnitStatus.OCCUPIED);
      Unit unit3 = unitWithIdentifier(SidGenerator.newUnitId(), "3", UnitStatus.VACANT);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit1, unit2, unit3));
      when(unitRepository.getByIdentifierAndTeamId(unit1Id, TEAM_ID)).thenReturn(unit1);
      when(unitRepository.getByIdentifierAndTeamId(unit2Id, TEAM_ID)).thenReturn(unit2);

      UpdateAllocationRequest request =
          new UpdateAllocationRequest(
              AllocationBasis.CUSTOM,
              Optional.of(
                  List.of(
                      new UnitShareEntry(unit1Id, new BigDecimal("60")),
                      new UnitShareEntry(unit2Id, new BigDecimal("40")))));

      assertThatThrownBy(() -> service.updateAllocation(PROPERTY_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("missing 1 of 3");

      verify(unitRepository, never()).save(any());
      verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUSTOM rejects shares that sum to 90 instead of 100, naming the actual total")
    void rejectsCustomShareSumMismatch() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);

      UnitIdentifier unit1Id = SidGenerator.newUnitId();
      UnitIdentifier unit2Id = SidGenerator.newUnitId();
      Unit unit1 = unitWithIdentifier(unit1Id, "1", UnitStatus.OCCUPIED);
      Unit unit2 = unitWithIdentifier(unit2Id, "2", UnitStatus.OCCUPIED);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(unit1, unit2));
      when(unitRepository.getByIdentifierAndTeamId(unit1Id, TEAM_ID)).thenReturn(unit1);
      when(unitRepository.getByIdentifierAndTeamId(unit2Id, TEAM_ID)).thenReturn(unit2);

      UpdateAllocationRequest request =
          new UpdateAllocationRequest(
              AllocationBasis.CUSTOM,
              Optional.of(
                  List.of(
                      new UnitShareEntry(unit1Id, new BigDecimal("50")),
                      new UnitShareEntry(unit2Id, new BigDecimal("40")))));

      assertThatThrownBy(() -> service.updateAllocation(PROPERTY_SID, request, principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("90");

      verify(unitRepository, never()).save(any());
      verify(propertyRepository, never()).save(any());
    }

    private Unit unitWithIdentifier(
        UnitIdentifier identifier, String unitNumber, UnitStatus status) {
      return Unit.builder()
          .id(UUID.randomUUID())
          .identifier(Optional.of(identifier))
          .teamId(TEAM_ID)
          .propertyId(PROPERTY_ID)
          .unitNumber(unitNumber)
          .unitType(UnitType.APARTMENT)
          .status(status)
          .build();
    }
  }

  @Nested
  @DisplayName("updateProperty residentialDetails (BUUR-106)")
  class UpdatePropertyResidentialDetails {

    @Test
    @DisplayName(
        "persists residentialDetails onto the property's sole unit and reflects them back on the"
            + " very next read")
    void updatesAndRoundTripsResidentialDetails() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);
      when(propertyRepository.save(any(Property.class))).thenReturn(property);

      Unit soleUnit = unit("1", UnitType.APARTMENT, UnitStatus.VACANT);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(soleUnit));
      when(unitMapper.toSummary(any(Unit.class)))
          .thenAnswer(
              inv -> {
                Unit u = inv.getArgument(0);
                return new UnitSummaryResponse(
                    u.getIdentifier().orElseThrow(),
                    u.getUnitNumber(),
                    u.getName(),
                    u.getUnitType(),
                    u.getStatus());
              });

      // A tiny in-memory stand-in for the unit_residential_details row: save() stores what it's
      // given, findByUnitIdAndTeamId() reads it back — enough to prove the write PropertyService
      // does actually lands where the read PropertyService does looks, without a real database.
      AtomicReference<UnitResidentialDetails> stored = new AtomicReference<>();
      when(unitResidentialDetailsRepository.findByUnitIdAndTeamId(
              eq(soleUnit.getId()), eq(TEAM_ID)))
          .thenAnswer(inv -> Optional.ofNullable(stored.get()));
      when(unitResidentialDetailsRepository.save(any(UnitResidentialDetails.class)))
          .thenAnswer(
              inv -> {
                UnitResidentialDetails d = inv.getArgument(0);
                stored.set(d);
                return d;
              });

      ResidentialDetailsRequest residential =
          new ResidentialDetailsRequest(
              Optional.of(2), Optional.of(1), Optional.of(false), Optional.of("no pets"));
      UpdatePropertyRequest request = updateRequest(Optional.of(residential));

      PropertyResponse response = service.updateProperty(PROPERTY_SID, request, principal);

      ArgumentCaptor<UnitResidentialDetails> captor =
          ArgumentCaptor.forClass(UnitResidentialDetails.class);
      verify(unitResidentialDetailsRepository).save(captor.capture());
      assertThat(captor.getValue().getUnitId()).isEqualTo(soleUnit.getId());
      assertThat(captor.getValue().getTeamId()).isEqualTo(TEAM_ID);

      assertThat(response.residentialDetails()).isPresent();
      assertThat(response.residentialDetails().get().bedrooms()).contains(2);
      assertThat(response.residentialDetails().get().bathrooms()).contains(1);
      assertThat(response.residentialDetails().get().furnished()).contains(false);
      assertThat(response.residentialDetails().get().petPolicy()).contains("no pets");
    }

    @Test
    @DisplayName("rejects residentialDetails with 400 when the property has more than one unit")
    void rejectsResidentialDetailsForMultiUnitProperty() {
      Property property = savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT);
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID)).thenReturn(property);

      List<Unit> units =
          List.of(
              unit("1", UnitType.APARTMENT, UnitStatus.OCCUPIED),
              unit("2", UnitType.APARTMENT, UnitStatus.VACANT));
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(units);
      when(unitMapper.toSummary(any(Unit.class)))
          .thenAnswer(
              inv -> {
                Unit u = inv.getArgument(0);
                return new UnitSummaryResponse(
                    u.getIdentifier().orElseThrow(),
                    u.getUnitNumber(),
                    u.getName(),
                    u.getUnitType(),
                    u.getStatus());
              });

      ResidentialDetailsRequest residential =
          new ResidentialDetailsRequest(
              Optional.of(2), Optional.of(1), Optional.of(false), Optional.empty());
      UpdatePropertyRequest request = updateRequest(Optional.of(residential));

      assertThatThrownBy(() -> service.updateProperty(PROPERTY_SID, request, principal))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("more than one unit");

      // The ambiguity is only in the residential-details write itself (which never happens);
      // the rest of the property update runs first and is not what this test is about.
      verify(unitResidentialDetailsRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("deleteProperty")
  class DeleteProperty {

    @Test
    @DisplayName(
        "cascades the soft delete to the property's units and their amenity links, in the same"
            + " transaction (BUUR-106 wave3c Critical 4)")
    void cascadesSoftDeleteToUnitsAndAmenityLinks() {
      when(propertyRepository.getByIdentifierAndTeamId(PROPERTY_SID, TEAM_ID))
          .thenReturn(savedProperty(PropertyCategory.RESIDENTIAL, PropertyType.APARTMENT));

      service.deleteProperty(PROPERTY_SID, principal);

      verify(propertyRepository).softDeleteByIdAndTeamId(PROPERTY_ID, TEAM_ID);
      verify(unitRepository).softDeleteAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID, USER_ID);
      verify(unitAmenityRepository)
          .softDeleteAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID, USER_ID);
    }
  }
}
