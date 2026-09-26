package com.buurman.service;

import static com.buurman.domain.NotificationType.PROPERTY_CREATED;
import static com.buurman.domain.Property.PropertyCategory.AGRICULTURAL;
import static com.buurman.domain.Property.PropertyCategory.COMMERCIAL;
import static com.buurman.domain.Property.PropertyCategory.INDUSTRIAL;
import static com.buurman.domain.Property.PropertyCategory.RESIDENTIAL;
import static com.buurman.domain.Property.PropertyType.APARTMENT;
import static com.buurman.domain.Property.PropertyType.AUTO_DEALERSHIP;
import static com.buurman.domain.Property.PropertyType.BAR;
import static com.buurman.domain.Property.PropertyType.BED_AND_BREAKFAST;
import static com.buurman.domain.Property.PropertyType.CAFE;
import static com.buurman.domain.Property.PropertyType.COLD_STORAGE;
import static com.buurman.domain.Property.PropertyType.DATA_CENTER;
import static com.buurman.domain.Property.PropertyType.FACTORY;
import static com.buurman.domain.Property.PropertyType.FARMLAND;
import static com.buurman.domain.Property.PropertyType.GARAGE;
import static com.buurman.domain.Property.PropertyType.GREENHOUSE;
import static com.buurman.domain.Property.PropertyType.HOTEL;
import static com.buurman.domain.Property.PropertyType.HOUSE;
import static com.buurman.domain.Property.PropertyType.MOTEL;
import static com.buurman.domain.Property.PropertyType.OFFICE;
import static com.buurman.domain.Property.PropertyType.ORCHARD;
import static com.buurman.domain.Property.PropertyType.OTHER_AGRICULTURAL;
import static com.buurman.domain.Property.PropertyType.OTHER_COMMERCIAL;
import static com.buurman.domain.Property.PropertyType.OTHER_INDUSTRIAL;
import static com.buurman.domain.Property.PropertyType.OTHER_RESIDENTIAL;
import static com.buurman.domain.Property.PropertyType.RANCH;
import static com.buurman.domain.Property.PropertyType.RESTAURANT;
import static com.buurman.domain.Property.PropertyType.RETAIL;
import static com.buurman.domain.Property.PropertyType.ROOM;
import static com.buurman.domain.Property.PropertyType.SHOWROOM;
import static com.buurman.domain.Property.PropertyType.SNACKBAR;
import static com.buurman.domain.Property.PropertyType.STUDIO;
import static com.buurman.domain.Property.PropertyType.TOWNHOUSE;
import static com.buurman.domain.Property.PropertyType.VILLA;
import static com.buurman.domain.Property.PropertyType.VINEYARD;
import static com.buurman.domain.Property.PropertyType.WAREHOUSE;
import static com.buurman.domain.Property.PropertyType.WORKSHOP;
import static com.buurman.util.SidGenerator.newPropertyId;

import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyType;
import com.buurman.domain.PropertyAgriculturalDetails;
import com.buurman.domain.PropertyCommercialDetails;
import com.buurman.domain.PropertyIndustrialDetails;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.AgriculturalDetailsRequest;
import com.buurman.dto.request.CommercialDetailsRequest;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.IndustrialDetailsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.ResidentialDetailsRequest;
import com.buurman.dto.request.UpdateAllocationRequest;
import com.buurman.dto.request.UpdateAllocationRequest.UnitShareEntry;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.AgriculturalDetailsResponse;
import com.buurman.dto.response.CommercialDetailsResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.IndustrialDetailsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.ResidentialDetailsResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyAgriculturalDetailsRepository;
import com.buurman.repository.PropertyCommercialDetailsRepository;
import com.buurman.repository.PropertyIndustrialDetailsRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final PropertyCommercialDetailsRepository commercialDetailsRepository;
  private final PropertyIndustrialDetailsRepository industrialDetailsRepository;
  private final PropertyAgriculturalDetailsRepository agriculturalDetailsRepository;
  private final PropertyMapper propertyMapper;
  private final AuditService auditService;
  private final DocumentService documentService;
  private final PhotoService photoService;
  private final PhotoRepository photoRepository;
  private final S3StorageService s3StorageService;
  private final PropertyOutdoorAreaRepository outdoorAreaRepository;
  private final MetricsService metricsService;
  private final NotificationService notificationService;
  private final AppProperties appProperties;
  private final GeocodingService geocodingService;

  private static final Map<PropertyCategory, Set<PropertyType>> VALID_TYPES_BY_CATEGORY;

  static {
    VALID_TYPES_BY_CATEGORY = new EnumMap<>(PropertyCategory.class);
    VALID_TYPES_BY_CATEGORY.put(
        RESIDENTIAL, Set.of(APARTMENT, HOUSE, STUDIO, ROOM, VILLA, TOWNHOUSE, OTHER_RESIDENTIAL));
    VALID_TYPES_BY_CATEGORY.put(
        COMMERCIAL,
        Set.of(
            OFFICE,
            RETAIL,
            RESTAURANT,
            HOTEL,
            SHOWROOM,
            AUTO_DEALERSHIP,
            SNACKBAR,
            CAFE,
            MOTEL,
            BAR,
            BED_AND_BREAKFAST,
            OTHER_COMMERCIAL));
    VALID_TYPES_BY_CATEGORY.put(
        INDUSTRIAL,
        Set.of(WAREHOUSE, WORKSHOP, FACTORY, DATA_CENTER, COLD_STORAGE, GARAGE, OTHER_INDUSTRIAL));
    VALID_TYPES_BY_CATEGORY.put(
        AGRICULTURAL, Set.of(FARMLAND, RANCH, GREENHOUSE, ORCHARD, VINEYARD, OTHER_AGRICULTURAL));
    VALID_TYPES_BY_CATEGORY.put(PropertyCategory.MIXED_USE, Set.of(PropertyType.MIXED_USE));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse createProperty(CreatePropertyRequest request, UserPrincipal principal) {
    validateCategoryTypeMatch(request.propertyCategory(), request.propertyType());

    Property property = propertyMapper.toEntity(request);
    property.setIdentifier(Optional.of(newPropertyId()));
    property.setTeamId(principal.requireTeamId());
    property.setCreatedBy(principal.getUserId());
    property.setUpdatedBy(principal.getUserId());

    if (property.getLatitude().isEmpty() || property.getLongitude().isEmpty()) {
      geocodingService
          .geocode(
              request.street(), request.city(),
              Optional.of(request.postalCode()), request.countryCode())
          .ifPresent(
              result -> {
                property.setLatitude(Optional.of(result.latitude()));
                property.setLongitude(Optional.of(result.longitude()));
                property.setGeocodeAccuracy(Optional.of(result.accuracy()));
              });
    }

    Property savedProperty = propertyRepository.save(property);

    saveDetailsForCategory(
        request.propertyCategory(),
        savedProperty.getId(),
        principal.requireTeamId(),
        principal.getUserId(),
        request.residentialDetails(),
        request.commercialDetails(),
        request.industrialDetails(),
        request.agriculturalDetails());

    metricsService.incrementCounter("property.total");

    log.info(
        "Property created: {} ({}) for team {}",
        savedProperty.getIdentifier().orElseThrow(),
        request.propertyCategory(),
        principal.requireTeamId());

    auditService.logCreate(
        principal.requireTeamId(),
        "PROPERTY",
        savedProperty.getId(),
        principal.getUserId(),
        savedProperty);

    String propertyName = savedProperty.getStreet() + ", " + savedProperty.getCity();
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(principal.requireTeamId()))
            .notificationType(PROPERTY_CREATED)
            .templateName("property-created")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "propertyAddress",
                    propertyName,
                    "propertyType",
                    savedProperty.getPropertyType().name(),
                    "baseUrl",
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return toResponseWithMainPhoto(savedProperty, principal.requireTeamId(), true);
  }

  public PageResponse<PropertyResponse> getPropertiesPaginated(
      UserPrincipal principal,
      @Nullable String status,
      @Nullable String category,
      @Nullable String query,
      PageRequest pageRequest) {
    PaginatedResult<Property> result =
        propertyRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(), status, category, query, pageRequest);
    List<PropertyResponse> responses =
        result.items().stream()
            .map(property -> toResponseWithMainPhoto(property, principal.requireTeamId(), false))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public PropertyResponse getProperty(PropertyIdentifier identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return toResponseWithMainPhoto(property, principal.requireTeamId(), true);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse updateProperty(
      PropertyIdentifier identifier, UpdatePropertyRequest request, UserPrincipal principal) {

    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    PropertyResponse oldState = toResponseWithMainPhoto(property, principal.requireTeamId(), true);

    // Category is immutable — validate type still matches
    validateCategoryTypeMatch(property.getPropertyCategory(), request.propertyType());

    String oldStreet = property.getStreet();
    String oldCity = property.getCity();
    String oldPostalCode = property.getPostalCode();
    String oldCountry = property.getCountryCode();

    propertyMapper.updateEntity(property, request);
    property.setUpdatedBy(principal.getUserId());

    boolean addressChanged =
        !Objects.equals(oldStreet, property.getStreet())
            || !Objects.equals(oldCity, property.getCity())
            || !Objects.equals(oldPostalCode, property.getPostalCode())
            || !Objects.equals(oldCountry, property.getCountryCode());

    if (addressChanged && (property.getLatitude().isEmpty() || property.getLongitude().isEmpty())) {
      geocodingService
          .geocode(
              property.getStreet(), property.getCity(),
              Optional.of(property.getPostalCode()), property.getCountryCode())
          .ifPresent(
              result -> {
                property.setLatitude(Optional.of(result.latitude()));
                property.setLongitude(Optional.of(result.longitude()));
                property.setGeocodeAccuracy(Optional.of(result.accuracy()));
              });
    }

    Property updatedProperty = propertyRepository.save(property);

    updateDetailsForCategory(
        property.getPropertyCategory(),
        property.getId(),
        principal.requireTeamId(),
        principal.getUserId(),
        request.residentialDetails(),
        request.commercialDetails(),
        request.industrialDetails(),
        request.agriculturalDetails());

    PropertyResponse newState =
        toResponseWithMainPhoto(updatedProperty, principal.requireTeamId(), true);

    log.info("Property updated: {} for team {}", identifier, principal.requireTeamId());

    auditService.logUpdate(
        principal.requireTeamId(),
        "PROPERTY",
        updatedProperty.getId(),
        principal.getUserId(),
        oldState,
        newState,
        auditService.getChangedFields(oldState, newState));

    return newState;
  }

  /**
   * Sets a property's allocation basis and, for CUSTOM, each unit's share. Does not retroactively
   * rewrite any expense's already-persisted allocations — call {@code
   * ExpenseService#recomputeExpenseAllocations} explicitly for that, since NL service-charge
   * settlement statements built from these rows are legal documents.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse updateAllocation(
      PropertyIdentifier identifier, UpdateAllocationRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(identifier, teamId);

    PropertyResponse oldState = toResponseWithMainPhoto(property, teamId, true);

    request
        .shares()
        .ifPresent(
            shares -> {
              for (UnitShareEntry share : shares) {
                Unit unit = unitRepository.getByIdentifierAndTeamId(share.unitIdentifier(), teamId);
                if (!unit.getPropertyId().equals(property.getId())) {
                  throw new BusinessRuleException(
                      "Unit "
                          + share.unitIdentifier().value()
                          + " does not belong to this property.");
                }
                unit.setAllocationShare(Optional.of(share.sharePct()));
                unit.setUpdatedBy(Optional.of(principal.getUserId()));
                unitRepository.save(unit);
              }
            });

    property.setAllocationBasis(request.basis());
    property.setUpdatedBy(principal.getUserId());
    Property updatedProperty = propertyRepository.save(property);

    PropertyResponse newState = toResponseWithMainPhoto(updatedProperty, teamId, true);

    log.info("Property allocation updated: {} for team {}", identifier, teamId);

    auditService.logUpdate(
        teamId,
        "PROPERTY",
        updatedProperty.getId(),
        principal.getUserId(),
        oldState,
        newState,
        auditService.getChangedFields(oldState, newState));

    return newState;
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteProperty(PropertyIdentifier identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    propertyRepository.softDeleteByIdAndTeamId(property.getId(), principal.requireTeamId());
    log.info("Property deleted: {} for team {}", identifier, principal.requireTeamId());

    auditService.logDelete(
        principal.requireTeamId(), "PROPERTY", property.getId(), principal.getUserId(), property);
  }

  public DocumentResponse uploadDocument(
      PropertyIdentifier identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file,
        "PROPERTY",
        property.getId(),
        property.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getDocuments(
      PropertyIdentifier identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.getDocuments("PROPERTY", property.getId(), principal);
  }

  public Map<String, String> getDownloadUrl(
      DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  public void deleteDocument(DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  public List<RecentActivityResponse> getAuditLog(
      PropertyIdentifier identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "PROPERTY", property.getId());
  }

  public List<PhotoResponse> getPhotos(PropertyIdentifier identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.getPhotos("PROPERTY", property.getId(), principal);
  }

  public PhotoResponse uploadPhoto(
      PropertyIdentifier identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.uploadPhoto(
        file,
        "PROPERTY",
        property.getId(),
        property.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public PhotoResponse setMainPhoto(
      PropertyIdentifier identifier, PhotoIdentifier photoIdentifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    Photo photo =
        photoRepository.getByIdentifierAndTeamId(photoIdentifier, principal.requireTeamId());
    return photoService.setMainPhoto(photo.getId(), "PROPERTY", property.getId(), principal);
  }

  // --- Private helpers ---
  private void validateCategoryTypeMatch(PropertyCategory category, PropertyType type) {
    Set<PropertyType> validTypes = VALID_TYPES_BY_CATEGORY.get(category);
    if (validTypes == null || !validTypes.contains(type)) {
      throw new BadRequestException(
          "Property type " + type + " is not valid for category " + category);
    }
  }

  private void saveDetailsForCategory(
      PropertyCategory category,
      UUID propertyId,
      UUID teamId,
      UUID userId,
      Optional<ResidentialDetailsRequest> residential,
      Optional<CommercialDetailsRequest> commercial,
      Optional<IndustrialDetailsRequest> industrial,
      Optional<AgriculturalDetailsRequest> agricultural) {
    switch (category) {
      case RESIDENTIAL -> {
        // TODO(BUUR-106 Task 9): property_residential_details was dropped in V068; residential
        // detail storage moves to unit_residential_details at unit level.
      }
      case COMMERCIAL ->
          commercial.ifPresent(
              c -> {
                PropertyCommercialDetails d = new PropertyCommercialDetails();
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setUsableAreaValue(c.usableAreaValue());
                d.setUsableAreaUnit(c.usableAreaUnit());
                d.setCommonAreaValue(c.commonAreaValue());
                d.setCommonAreaUnit(c.commonAreaUnit());
                d.setFloorLevel(c.floorLevel());
                d.setCeilingHeightValue(c.ceilingHeightValue());
                d.setCeilingHeightUnit(c.ceilingHeightUnit());
                d.setHasStorefront(Optional.of(c.hasStorefront().orElse(false)));
                d.setHasSignageRights(Optional.of(c.hasSignageRights().orElse(false)));
                d.setZoningClassification(c.zoningClassification());
                d.setMaxOccupancy(c.maxOccupancy());
                d.setRestroomCount(c.restroomCount());
                d.setHasKitchenFacility(Optional.of(c.hasKitchenFacility().orElse(false)));
                d.setAccessibilityCompliant(Optional.of(c.accessibilityCompliant().orElse(false)));
                d.setCreatedBy(userId);
                d.setUpdatedBy(userId);
                commercialDetailsRepository.save(d);
              });
      case INDUSTRIAL ->
          industrial.ifPresent(
              i -> {
                PropertyIndustrialDetails d = new PropertyIndustrialDetails();
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setClearHeightValue(i.clearHeightValue());
                d.setClearHeightUnit(i.clearHeightUnit());
                d.setLoadingDocks(i.loadingDocks());
                d.setDriveInDoors(i.driveInDoors());
                d.setFloorLoadCapacityValue(i.floorLoadCapacityValue());
                d.setFloorLoadCapacityUnit(i.floorLoadCapacityUnit());
                d.setPowerCapacityValue(i.powerCapacityValue());
                d.setPowerCapacityUnit(i.powerCapacityUnit());
                d.setHasThreePhasePower(Optional.of(i.hasThreePhasePower().orElse(false)));
                d.setHasCrane(Optional.of(i.hasCrane().orElse(false)));
                d.setCraneCapacityValue(i.craneCapacityValue());
                d.setCraneCapacityUnit(i.craneCapacityUnit());
                d.setHasHazmatCertification(Optional.of(i.hasHazmatCertification().orElse(false)));
                d.setHasVentilationSystem(Optional.of(i.hasVentilationSystem().orElse(false)));
                d.setHasClimateControl(Optional.of(i.hasClimateControl().orElse(false)));
                d.setYardAreaValue(i.yardAreaValue());
                d.setYardAreaUnit(i.yardAreaUnit());
                d.setZoningClassification(i.zoningClassification());
                d.setCreatedBy(userId);
                d.setUpdatedBy(userId);
                industrialDetailsRepository.save(d);
              });
      case AGRICULTURAL ->
          agricultural.ifPresent(
              a -> {
                PropertyAgriculturalDetails d = new PropertyAgriculturalDetails();
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setTotalLandAreaValue(a.totalLandAreaValue());
                d.setTotalLandAreaUnit(a.totalLandAreaUnit());
                d.setArableAreaValue(a.arableAreaValue());
                d.setArableAreaUnit(a.arableAreaUnit());
                d.setSoilType(a.soilType());
                d.setHasWaterRights(Optional.of(a.hasWaterRights().orElse(false)));
                d.setWaterSource(a.waterSource());
                d.setIrrigationType(a.irrigationType());
                d.setFencingType(a.fencingType());
                d.setHasOutbuildings(Optional.of(a.hasOutbuildings().orElse(false)));
                d.setOutbuildingDetails(a.outbuildingDetails());
                d.setCurrentUse(a.currentUse());
                d.setZoningClassification(a.zoningClassification());
                d.setCreatedBy(userId);
                d.setUpdatedBy(userId);
                agriculturalDetailsRepository.save(d);
              });
      case MIXED_USE -> {
        // No detail table for MIXED_USE
      }
    }
  }

  private void updateDetailsForCategory(
      PropertyCategory category,
      UUID propertyId,
      UUID teamId,
      UUID userId,
      Optional<ResidentialDetailsRequest> residential,
      Optional<CommercialDetailsRequest> commercial,
      Optional<IndustrialDetailsRequest> industrial,
      Optional<AgriculturalDetailsRequest> agricultural) {
    switch (category) {
      case RESIDENTIAL -> {
        // TODO(BUUR-106 Task 9): property_residential_details was dropped in V068; residential
        // detail storage moves to unit_residential_details at unit level.
      }
      case COMMERCIAL ->
          commercial.ifPresent(
              c -> {
                Optional<PropertyCommercialDetails> existing =
                    commercialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
                PropertyCommercialDetails d = existing.orElseGet(PropertyCommercialDetails::new);
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setUsableAreaValue(c.usableAreaValue());
                d.setUsableAreaUnit(c.usableAreaUnit());
                d.setCommonAreaValue(c.commonAreaValue());
                d.setCommonAreaUnit(c.commonAreaUnit());
                d.setFloorLevel(c.floorLevel());
                d.setCeilingHeightValue(c.ceilingHeightValue());
                d.setCeilingHeightUnit(c.ceilingHeightUnit());
                d.setHasStorefront(Optional.of(c.hasStorefront().orElse(false)));
                d.setHasSignageRights(Optional.of(c.hasSignageRights().orElse(false)));
                d.setZoningClassification(c.zoningClassification());
                d.setMaxOccupancy(c.maxOccupancy());
                d.setRestroomCount(c.restroomCount());
                d.setHasKitchenFacility(Optional.of(c.hasKitchenFacility().orElse(false)));
                d.setAccessibilityCompliant(Optional.of(c.accessibilityCompliant().orElse(false)));
                d.setUpdatedBy(userId);
                if (d.getId() == null) {
                  d.setCreatedBy(userId);
                }
                commercialDetailsRepository.save(d);
              });
      case INDUSTRIAL ->
          industrial.ifPresent(
              i -> {
                Optional<PropertyIndustrialDetails> existing =
                    industrialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
                PropertyIndustrialDetails d = existing.orElseGet(PropertyIndustrialDetails::new);
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setClearHeightValue(i.clearHeightValue());
                d.setClearHeightUnit(i.clearHeightUnit());
                d.setLoadingDocks(i.loadingDocks());
                d.setDriveInDoors(i.driveInDoors());
                d.setFloorLoadCapacityValue(i.floorLoadCapacityValue());
                d.setFloorLoadCapacityUnit(i.floorLoadCapacityUnit());
                d.setPowerCapacityValue(i.powerCapacityValue());
                d.setPowerCapacityUnit(i.powerCapacityUnit());
                d.setHasThreePhasePower(Optional.of(i.hasThreePhasePower().orElse(false)));
                d.setHasCrane(Optional.of(i.hasCrane().orElse(false)));
                d.setCraneCapacityValue(i.craneCapacityValue());
                d.setCraneCapacityUnit(i.craneCapacityUnit());
                d.setHasHazmatCertification(Optional.of(i.hasHazmatCertification().orElse(false)));
                d.setHasVentilationSystem(Optional.of(i.hasVentilationSystem().orElse(false)));
                d.setHasClimateControl(Optional.of(i.hasClimateControl().orElse(false)));
                d.setYardAreaValue(i.yardAreaValue());
                d.setYardAreaUnit(i.yardAreaUnit());
                d.setZoningClassification(i.zoningClassification());
                d.setUpdatedBy(userId);
                if (d.getId() == null) {
                  d.setCreatedBy(userId);
                }
                industrialDetailsRepository.save(d);
              });
      case AGRICULTURAL ->
          agricultural.ifPresent(
              a -> {
                Optional<PropertyAgriculturalDetails> existing =
                    agriculturalDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
                PropertyAgriculturalDetails d =
                    existing.orElseGet(PropertyAgriculturalDetails::new);
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setTotalLandAreaValue(a.totalLandAreaValue());
                d.setTotalLandAreaUnit(a.totalLandAreaUnit());
                d.setArableAreaValue(a.arableAreaValue());
                d.setArableAreaUnit(a.arableAreaUnit());
                d.setSoilType(a.soilType());
                d.setHasWaterRights(Optional.of(a.hasWaterRights().orElse(false)));
                d.setWaterSource(a.waterSource());
                d.setIrrigationType(a.irrigationType());
                d.setFencingType(a.fencingType());
                d.setHasOutbuildings(Optional.of(a.hasOutbuildings().orElse(false)));
                d.setOutbuildingDetails(a.outbuildingDetails());
                d.setCurrentUse(a.currentUse());
                d.setZoningClassification(a.zoningClassification());
                d.setUpdatedBy(userId);
                if (d.getId() == null) {
                  d.setCreatedBy(userId);
                }
                agriculturalDetailsRepository.save(d);
              });
      case MIXED_USE -> {
        // No detail table
      }
    }
  }

  private Optional<CommercialDetailsResponse> buildCommercialResponse(
      UUID propertyId, UUID teamId) {
    return commercialDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new CommercialDetailsResponse(
                    d.getUsableAreaValue(),
                    d.getUsableAreaUnit(),
                    d.getCommonAreaValue(),
                    d.getCommonAreaUnit(),
                    d.getFloorLevel(),
                    d.getCeilingHeightValue(),
                    d.getCeilingHeightUnit(),
                    d.getHasStorefront(),
                    d.getHasSignageRights(),
                    d.getZoningClassification(),
                    d.getMaxOccupancy(),
                    d.getRestroomCount(),
                    d.getHasKitchenFacility(),
                    d.getAccessibilityCompliant()));
  }

  private Optional<IndustrialDetailsResponse> buildIndustrialResponse(
      UUID propertyId, UUID teamId) {
    return industrialDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new IndustrialDetailsResponse(
                    d.getClearHeightValue(),
                    d.getClearHeightUnit(),
                    d.getLoadingDocks(),
                    d.getDriveInDoors(),
                    d.getFloorLoadCapacityValue(),
                    d.getFloorLoadCapacityUnit(),
                    d.getPowerCapacityValue(),
                    d.getPowerCapacityUnit(),
                    d.getHasThreePhasePower(),
                    d.getHasCrane(),
                    d.getCraneCapacityValue(),
                    d.getCraneCapacityUnit(),
                    d.getHasHazmatCertification(),
                    d.getHasVentilationSystem(),
                    d.getHasClimateControl(),
                    d.getYardAreaValue(),
                    d.getYardAreaUnit(),
                    d.getZoningClassification()));
  }

  private Optional<AgriculturalDetailsResponse> buildAgriculturalResponse(
      UUID propertyId, UUID teamId) {
    return agriculturalDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new AgriculturalDetailsResponse(
                    d.getTotalLandAreaValue(),
                    d.getTotalLandAreaUnit(),
                    d.getArableAreaValue(),
                    d.getArableAreaUnit(),
                    d.getSoilType(),
                    d.getHasWaterRights(),
                    d.getWaterSource(),
                    d.getIrrigationType(),
                    d.getFencingType(),
                    d.getHasOutbuildings(),
                    d.getOutbuildingDetails(),
                    d.getCurrentUse(),
                    d.getZoningClassification()));
  }

  private PropertyResponse toResponseWithMainPhoto(
      Property property, UUID teamId, boolean includeNestedCollections) {
    PropertyResponse response = propertyMapper.toResponse(property);

    List<Photo> photos =
        photoRepository.findByEntityAndTeamId("PROPERTY", property.getId(), teamId);

    Optional<Photo> mainPhoto = photos.stream().filter(Photo::getIsMainPhoto).findFirst();

    Optional<String> mainPhotoUrl =
        mainPhoto.map(
            photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString());

    Optional<String> mainPhotoThumbnailUrl =
        mainPhoto.map(
            photo -> {
              String key = photo.getThumbnailFileKey().orElse(photo.getFileKey());
              return s3StorageService.generatePresignedUrl(key).toString();
            });

    Optional<List<PropertyOutdoorAreaResponse>> outdoorAreas =
        includeNestedCollections
            ? Optional.of(
                outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
                    .map(
                        a ->
                            new PropertyOutdoorAreaResponse(
                                a.getIdentifier().orElseThrow(),
                                a.getType(),
                                a.getAreaValue(),
                                Optional.of(a.getAreaUnit()),
                                a.getCreatedAt(),
                                Optional.of(a.getUpdatedAt())))
                    .toList())
            : Optional.empty();

    // TODO(BUUR-106 Task 9): property_amenities was dropped in V068; amenity links move to
    // unit_amenities at unit level. Always empty until the unit-level endpoint is restored.
    Optional<List<PropertyAmenityResponse>> amenities = Optional.empty();

    // Build category-specific detail responses
    // TODO(BUUR-106 Task 9): property_residential_details was dropped in V068; residential
    // details move to unit_residential_details at unit level.
    Optional<ResidentialDetailsResponse> residentialDetails = Optional.empty();
    Optional<CommercialDetailsResponse> commercialDetails = Optional.empty();
    Optional<IndustrialDetailsResponse> industrialDetails = Optional.empty();
    Optional<AgriculturalDetailsResponse> agriculturalDetails = Optional.empty();

    switch (property.getPropertyCategory()) {
      case RESIDENTIAL -> {
        // No-op: see TODO above.
      }
      case COMMERCIAL -> commercialDetails = buildCommercialResponse(property.getId(), teamId);
      case INDUSTRIAL -> industrialDetails = buildIndustrialResponse(property.getId(), teamId);
      case AGRICULTURAL ->
          agriculturalDetails = buildAgriculturalResponse(property.getId(), teamId);
      case MIXED_USE -> {
        // No detail table
      }
    }

    return new PropertyResponse(
        response.identifier(),
        response.propertyCategory(),
        response.propertyType(),
        response.allocationBasis(),
        response.street(),
        response.city(),
        response.postalCode(),
        response.countryCode(),
        response.regionCode(),
        response.latitude(),
        response.longitude(),
        response.geocodeAccuracy(),
        response.areaValue(),
        response.areaUnit(),
        mainPhotoUrl,
        mainPhotoThumbnailUrl,
        // Construction & Structure
        response.yearBuilt(),
        response.yearLastRenovated(),
        response.constructionType(),
        response.foundationType(),
        response.roofType(),
        response.wallConstruction(),
        response.flooringType(),
        response.windowType(),
        response.numberOfFloors(),
        response.structuralNotes(),
        // Energy & Climate
        response.energyEfficiencyRating(),
        response.energyCertificateExpiryDate(),
        response.heatingType(),
        response.coolingType(),
        response.hotWaterSystem(),
        response.insulationNotes(),
        // Utilities & Connections
        response.electricityConnectionType(),
        response.electricityCapacityValue(),
        response.electricityCapacityUnit(),
        response.waterConnectionType(),
        response.hasGasConnection(),
        response.sewageType(),
        response.internetConnectionType(),
        response.internetMaxSpeedValue(),
        response.internetMaxSpeedUnit(),
        response.internetStatus(),
        // Parking
        response.parkingSpaces(),
        response.parkingType(),
        // Safety & Security
        response.hasSmokeDetectors(),
        response.hasCoDetectors(),
        response.hasFireExtinguisher(),
        response.hasSprinklerSystem(),
        response.hasAlarmSystem(),
        response.hasSecurityCameras(),
        response.hasSecureEntry(),
        response.safetyNotes(),
        // Accessibility
        response.isWheelchairAccessible(),
        response.hasElevator(),
        response.hasStepFreeEntrance(),
        response.hasAdaptedBathroom(),
        response.accessibilityNotes(),
        // Category-specific details
        residentialDetails,
        commercialDetails,
        industrialDetails,
        agriculturalDetails,
        // Nested collections
        outdoorAreas,
        amenities,
        response.createdAt(),
        response.updatedAt());
  }
}
