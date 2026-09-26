package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.UnitSummaryResponse;
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
import com.buurman.repository.UnitRepository;
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
        Optional.empty(),
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
}
