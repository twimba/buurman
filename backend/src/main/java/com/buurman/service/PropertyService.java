package com.buurman.service;

import static com.buurman.util.UlidGenerator.newPropertyId;

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
import com.buurman.domain.NotificationType;
import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyType;
import com.buurman.domain.PropertyAgriculturalDetails;
import com.buurman.domain.PropertyCommercialDetails;
import com.buurman.domain.PropertyIndustrialDetails;
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.dto.request.AgriculturalDetailsRequest;
import com.buurman.dto.request.CommercialDetailsRequest;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.IndustrialDetailsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.ResidentialDetailsRequest;
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
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyAgriculturalDetailsRepository;
import com.buurman.repository.PropertyCommercialDetailsRepository;
import com.buurman.repository.PropertyIndustrialDetailsRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;
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
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
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
  private final PropertyAmenityService propertyAmenityService;
  private final MetricsService metricsService;
  private final NotificationService notificationService;
  private final AppProperties appProperties;
  private final GeocodingService geocodingService;

  private static final Map<PropertyCategory, Set<PropertyType>> VALID_TYPES_BY_CATEGORY;

  static {
    VALID_TYPES_BY_CATEGORY = new EnumMap<>(PropertyCategory.class);
    VALID_TYPES_BY_CATEGORY.put(
        PropertyCategory.RESIDENTIAL,
        Set.of(
            PropertyType.APARTMENT,
            PropertyType.HOUSE,
            PropertyType.STUDIO,
            PropertyType.ROOM,
            PropertyType.VILLA,
            PropertyType.TOWNHOUSE,
            PropertyType.OTHER_RESIDENTIAL));
    VALID_TYPES_BY_CATEGORY.put(
        PropertyCategory.COMMERCIAL,
        Set.of(
            PropertyType.OFFICE,
            PropertyType.RETAIL,
            PropertyType.RESTAURANT,
            PropertyType.HOTEL,
            PropertyType.SHOWROOM,
            PropertyType.AUTO_DEALERSHIP,
            PropertyType.SNACKBAR,
            PropertyType.CAFE,
            PropertyType.MOTEL,
            PropertyType.BAR,
            PropertyType.BED_AND_BREAKFAST,
            PropertyType.OTHER_COMMERCIAL));
    VALID_TYPES_BY_CATEGORY.put(
        PropertyCategory.INDUSTRIAL,
        Set.of(
            PropertyType.WAREHOUSE,
            PropertyType.WORKSHOP,
            PropertyType.FACTORY,
            PropertyType.DATA_CENTER,
            PropertyType.COLD_STORAGE,
            PropertyType.GARAGE,
            PropertyType.OTHER_INDUSTRIAL));
    VALID_TYPES_BY_CATEGORY.put(
        PropertyCategory.AGRICULTURAL,
        Set.of(
            PropertyType.FARMLAND,
            PropertyType.RANCH,
            PropertyType.GREENHOUSE,
            PropertyType.ORCHARD,
            PropertyType.VINEYARD,
            PropertyType.OTHER_AGRICULTURAL));
    VALID_TYPES_BY_CATEGORY.put(PropertyCategory.MIXED_USE, Set.of(PropertyType.MIXED_USE));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse createProperty(CreatePropertyRequest request, UserPrincipal principal) {
    validateCategoryTypeMatch(request.propertyCategory(), request.propertyType());

    Property property = propertyMapper.toEntity(request);
    property.setIdentifier(newPropertyId().value());
    property.setTeamId(principal.requireTeamId());
    property.setCreatedBy(principal.getUserId());
    property.setUpdatedBy(principal.getUserId());

    if (property.getLatitude().isEmpty() || property.getLongitude().isEmpty()) {
      geocodingService
          .geocode(
              request.street(), request.city(),
              request.postalCode(), request.country())
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
        savedProperty.getIdentifier(),
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
            .teamId(principal.requireTeamId())
            .notificationType(NotificationType.PROPERTY_CREATED)
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

  public PropertyResponse getProperty(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return toResponseWithMainPhoto(property, principal.requireTeamId(), true);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse updateProperty(
      String identifier, UpdatePropertyRequest request, UserPrincipal principal) {

    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    PropertyResponse oldState = toResponseWithMainPhoto(property, principal.requireTeamId(), true);

    // Category is immutable — validate type still matches
    validateCategoryTypeMatch(property.getPropertyCategory(), request.propertyType());

    String oldStreet = property.getStreet();
    String oldCity = property.getCity();
    String oldPostalCode = property.getPostalCode();
    String oldCountry = property.getCountry();

    propertyMapper.updateEntity(property, request);
    property.setUpdatedBy(principal.getUserId());

    boolean addressChanged =
        !Objects.equals(oldStreet, property.getStreet())
            || !Objects.equals(oldCity, property.getCity())
            || !Objects.equals(oldPostalCode, property.getPostalCode())
            || !Objects.equals(oldCountry, property.getCountry());

    if (addressChanged && (property.getLatitude().isEmpty() || property.getLongitude().isEmpty())) {
      geocodingService
          .geocode(
              property.getStreet(), property.getCity(),
              property.getPostalCode(), property.getCountry())
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

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteProperty(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    propertyRepository.softDeleteByIdAndTeamId(property.getId(), principal.requireTeamId());
    log.info("Property deleted: {} for team {}", identifier, principal.requireTeamId());

    auditService.logDelete(
        principal.requireTeamId(), "PROPERTY", property.getId(), principal.getUserId(), property);
  }

  public DocumentResponse uploadDocument(
      String identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file, "PROPERTY", property.getId(), property.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getDocuments(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.getDocuments("PROPERTY", property.getId(), principal);
  }

  public Map<String, String> getDownloadUrl(String documentIdentifier, UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  public void deleteDocument(String documentIdentifier, UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  public List<RecentActivityResponse> getAuditLog(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "PROPERTY", property.getId());
  }

  public List<PhotoResponse> getPhotos(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.getPhotos("PROPERTY", property.getId(), principal);
  }

  public PhotoResponse uploadPhoto(
      String identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.uploadPhoto(
        file, "PROPERTY", property.getId(), property.getIdentifier(), title, notes, principal);
  }

  public PhotoResponse setMainPhoto(
      String identifier, String photoIdentifier, UserPrincipal principal) {
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
      case RESIDENTIAL ->
          residential.ifPresent(
              r -> {
                PropertyResidentialDetails d = new PropertyResidentialDetails();
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setBedrooms(r.bedrooms());
                d.setBathrooms(r.bathrooms());
                d.setFurnished(Optional.of(r.furnished().orElse(false)));
                d.setPetPolicy(r.petPolicy());
                d.setCreatedBy(userId);
                d.setUpdatedBy(userId);
                residentialDetailsRepository.save(d);
              });
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
                d.setPowerCapacityKva(i.powerCapacityKva());
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
      case RESIDENTIAL ->
          residential.ifPresent(
              r -> {
                Optional<PropertyResidentialDetails> existing =
                    residentialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
                PropertyResidentialDetails d = existing.orElseGet(PropertyResidentialDetails::new);
                d.setPropertyId(propertyId);
                d.setTeamId(teamId);
                d.setBedrooms(r.bedrooms());
                d.setBathrooms(r.bathrooms());
                d.setFurnished(Optional.of(r.furnished().orElse(false)));
                d.setPetPolicy(r.petPolicy());
                d.setUpdatedBy(userId);
                if (d.getId() == null) {
                  d.setCreatedBy(userId);
                }
                residentialDetailsRepository.save(d);
              });
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
                d.setPowerCapacityKva(i.powerCapacityKva());
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

  private Optional<ResidentialDetailsResponse> buildResidentialResponse(
      UUID propertyId, UUID teamId) {
    return residentialDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new ResidentialDetailsResponse(
                    d.getBedrooms(), d.getBathrooms(), d.getFurnished(), d.getPetPolicy()));
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
                    d.getPowerCapacityKva(),
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
                                a.getIdentifier(),
                                a.getType(),
                                a.getAreaValue(),
                                Optional.of(a.getAreaUnit()),
                                a.getCreatedAt(),
                                Optional.of(a.getUpdatedAt())))
                    .toList())
            : Optional.empty();

    Optional<List<PropertyAmenityResponse>> amenities =
        includeNestedCollections
            ? Optional.of(
                propertyAmenityService.buildPropertyAmenityResponses(property.getId(), teamId))
            : Optional.empty();

    // Build category-specific detail responses
    Optional<ResidentialDetailsResponse> residentialDetails = Optional.empty();
    Optional<CommercialDetailsResponse> commercialDetails = Optional.empty();
    Optional<IndustrialDetailsResponse> industrialDetails = Optional.empty();
    Optional<AgriculturalDetailsResponse> agriculturalDetails = Optional.empty();

    switch (property.getPropertyCategory()) {
      case RESIDENTIAL -> residentialDetails = buildResidentialResponse(property.getId(), teamId);
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
        response.status(),
        response.street(),
        response.city(),
        response.postalCode(),
        response.country(),
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
        response.electricityCapacityAmps(),
        response.waterConnectionType(),
        response.hasGasConnection(),
        response.sewageType(),
        response.internetConnectionType(),
        response.internetMaxSpeedMbps(),
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
