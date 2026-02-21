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
    validateCurrencyRequired(request.purchasePriceCurrency(), request.purchasePrice());
    validateCurrencyRequired(request.currentMarketValueCurrency(), request.currentMarketValue());
    validateCurrencyRequired(request.mortgageAmountCurrency(), request.mortgageAmount());
    validateCurrencyRequired(
        request.monthlyMortgagePaymentCurrency(), request.monthlyMortgagePayment());
    validateCurrencyRequired(request.annualPropertyTaxCurrency(), request.annualPropertyTax());
    validateCurrencyRequired(request.annualInsuranceCurrency(), request.annualInsurance());
    validateCurrencyRequired(request.annualHoaFeeCurrency(), request.annualHoaFee());
    validateCurrencyRequired(request.annualManagementFeeCurrency(), request.annualManagementFee());
    validateCurrencyRequired(
        request.annualMaintenanceReserveCurrency(), request.annualMaintenanceReserve());
    validateCurrencyRequired(request.landValueCurrency(), request.landValue());

    Property property = propertyMapper.toEntity(request);
    property.setIdentifier(newPropertyId().value());
    property.setTeamId(principal.getTeamId());
    property.setCreatedBy(principal.getUserId());
    property.setUpdatedBy(principal.getUserId());

    if (property.getLatitude() == null || property.getLongitude() == null) {
      geocodingService
          .geocode(
              request.street(), request.city(),
              request.postalCode(), request.country())
          .ifPresent(
              result -> {
                property.setLatitude(result.latitude());
                property.setLongitude(result.longitude());
                property.setGeocodeAccuracy(result.accuracy());
              });
    }

    Property savedProperty = propertyRepository.save(property);

    saveDetailsForCategory(
        request.propertyCategory(),
        savedProperty.getId(),
        principal.getTeamId(),
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
        principal.getTeamId());

    auditService.logCreate(
        principal.getTeamId(),
        "PROPERTY",
        savedProperty.getId(),
        principal.getUserId(),
        savedProperty);

    String propertyName =
        savedProperty.getStreet() != null
            ? savedProperty.getStreet() + ", " + savedProperty.getCity()
            : savedProperty.getIdentifier();
    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(principal.getTeamId())
            .notificationType(NotificationType.PROPERTY_CREATED)
            .templateName("property-created")
            .templateVariables(
                Map.of(
                    "propertyName",
                    propertyName,
                    "propertyAddress",
                    propertyName,
                    "propertyType",
                    savedProperty.getPropertyType() != null
                        ? savedProperty.getPropertyType().name()
                        : "N/A",
                    "baseUrl",
                    appProperties.email().baseUrl()))
            .createdBy(principal.getUserId())
            .build());

    return toResponseWithMainPhoto(savedProperty, principal.getTeamId(), true);
  }

  public List<PropertyResponse> getProperties(
      UserPrincipal principal, Property.PropertyStatus status) {
    List<Property> properties;
    if (status != null) {
      properties = propertyRepository.findByTeamIdAndStatus(principal.getTeamId(), status);
    } else {
      properties = propertyRepository.findAllByTeamId(principal.getTeamId());
    }

    return properties.stream()
        .map(property -> toResponseWithMainPhoto(property, principal.getTeamId(), false))
        .toList();
  }

  public PageResponse<PropertyResponse> getPropertiesPaginated(
      UserPrincipal principal,
      String status,
      String category,
      String query,
      PageRequest pageRequest) {
    PaginatedResult<Property> result =
        propertyRepository.findAllByTeamIdPaginated(
            principal.getTeamId(), status, category, query, pageRequest);
    List<PropertyResponse> responses =
        result.items().stream()
            .map(property -> toResponseWithMainPhoto(property, principal.getTeamId(), false))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public PropertyResponse getProperty(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return toResponseWithMainPhoto(property, principal.getTeamId(), true);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyResponse updateProperty(
      String identifier, UpdatePropertyRequest request, UserPrincipal principal) {

    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    PropertyResponse oldState = toResponseWithMainPhoto(property, principal.getTeamId(), true);

    // Category is immutable — validate type still matches
    validateCategoryTypeMatch(property.getPropertyCategory(), request.propertyType());
    validateCurrencyRequired(request.purchasePriceCurrency(), request.purchasePrice());
    validateCurrencyRequired(request.currentMarketValueCurrency(), request.currentMarketValue());
    validateCurrencyRequired(request.mortgageAmountCurrency(), request.mortgageAmount());
    validateCurrencyRequired(
        request.monthlyMortgagePaymentCurrency(), request.monthlyMortgagePayment());
    validateCurrencyRequired(request.annualPropertyTaxCurrency(), request.annualPropertyTax());
    validateCurrencyRequired(request.annualInsuranceCurrency(), request.annualInsurance());
    validateCurrencyRequired(request.annualHoaFeeCurrency(), request.annualHoaFee());
    validateCurrencyRequired(request.annualManagementFeeCurrency(), request.annualManagementFee());
    validateCurrencyRequired(
        request.annualMaintenanceReserveCurrency(), request.annualMaintenanceReserve());
    validateCurrencyRequired(request.landValueCurrency(), request.landValue());

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

    if (addressChanged && (property.getLatitude() == null || property.getLongitude() == null)) {
      geocodingService
          .geocode(
              property.getStreet(), property.getCity(),
              property.getPostalCode(), property.getCountry())
          .ifPresent(
              result -> {
                property.setLatitude(result.latitude());
                property.setLongitude(result.longitude());
                property.setGeocodeAccuracy(result.accuracy());
              });
    }

    Property updatedProperty = propertyRepository.save(property);

    updateDetailsForCategory(
        property.getPropertyCategory(),
        property.getId(),
        principal.getTeamId(),
        principal.getUserId(),
        request.residentialDetails(),
        request.commercialDetails(),
        request.industrialDetails(),
        request.agriculturalDetails());

    PropertyResponse newState =
        toResponseWithMainPhoto(updatedProperty, principal.getTeamId(), true);

    log.info("Property updated: {} for team {}", identifier, principal.getTeamId());

    auditService.logUpdate(
        principal.getTeamId(),
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
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    propertyRepository.softDeleteByIdAndTeamId(property.getId(), principal.getTeamId());
    log.info("Property deleted: {} for team {}", identifier, principal.getTeamId());

    auditService.logDelete(
        principal.getTeamId(), "PROPERTY", property.getId(), principal.getUserId(), property);
  }

  public DocumentResponse uploadDocument(
      String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return documentService.uploadDocument(
        file, "PROPERTY", property.getId(), property.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getDocuments(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
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
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return auditService.getEntityAuditLog(principal.getTeamId(), "PROPERTY", property.getId());
  }

  public List<PhotoResponse> getPhotos(String identifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return photoService.getPhotos("PROPERTY", property.getId(), principal);
  }

  public PhotoResponse uploadPhoto(
      String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return photoService.uploadPhoto(
        file, "PROPERTY", property.getId(), property.getIdentifier(), title, notes, principal);
  }

  public PhotoResponse setMainPhoto(
      String identifier, String photoIdentifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    Photo photo = photoRepository.getByIdentifierAndTeamId(photoIdentifier, principal.getTeamId());
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

  private void validateCurrencyRequired(String currency, java.math.BigDecimal... monetaryFields) {
    if (currency != null && !currency.isBlank()) {
      try {
        java.util.Currency.getInstance(currency);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid ISO 4217 currency code: " + currency);
      }
      return;
    }
    for (java.math.BigDecimal field : monetaryFields) {
      if (field != null) {
        throw new BadRequestException("Currency is required when monetary fields are provided");
      }
    }
  }

  private void saveDetailsForCategory(
      PropertyCategory category,
      UUID propertyId,
      UUID teamId,
      UUID userId,
      ResidentialDetailsRequest residential,
      CommercialDetailsRequest commercial,
      IndustrialDetailsRequest industrial,
      AgriculturalDetailsRequest agricultural) {
    switch (category) {
      case RESIDENTIAL -> {
        if (residential != null) {
          PropertyResidentialDetails d = new PropertyResidentialDetails();
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setBedrooms(residential.bedrooms());
          d.setBathrooms(residential.bathrooms());
          d.setFurnished(residential.furnished() != null ? residential.furnished() : false);
          d.setPetPolicy(residential.petPolicy());
          d.setCreatedBy(userId);
          d.setUpdatedBy(userId);
          residentialDetailsRepository.save(d);
        }
      }
      case COMMERCIAL -> {
        if (commercial != null) {
          PropertyCommercialDetails d = new PropertyCommercialDetails();
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setUsableAreaValue(commercial.usableAreaValue());
          d.setUsableAreaUnit(commercial.usableAreaUnit());
          d.setCommonAreaValue(commercial.commonAreaValue());
          d.setCommonAreaUnit(commercial.commonAreaUnit());
          d.setFloorLevel(commercial.floorLevel());
          d.setCeilingHeightM(commercial.ceilingHeightM());
          d.setHasStorefront(
              commercial.hasStorefront() != null ? commercial.hasStorefront() : false);
          d.setHasSignageRights(
              commercial.hasSignageRights() != null ? commercial.hasSignageRights() : false);
          d.setZoningClassification(commercial.zoningClassification());
          d.setMaxOccupancy(commercial.maxOccupancy());
          d.setRestroomCount(commercial.restroomCount());
          d.setHasKitchenFacility(
              commercial.hasKitchenFacility() != null ? commercial.hasKitchenFacility() : false);
          d.setAccessibilityCompliant(
              commercial.accessibilityCompliant() != null
                  ? commercial.accessibilityCompliant()
                  : false);
          d.setCreatedBy(userId);
          d.setUpdatedBy(userId);
          commercialDetailsRepository.save(d);
        }
      }
      case INDUSTRIAL -> {
        if (industrial != null) {
          PropertyIndustrialDetails d = new PropertyIndustrialDetails();
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setClearHeightM(industrial.clearHeightM());
          d.setLoadingDocks(industrial.loadingDocks());
          d.setDriveInDoors(industrial.driveInDoors());
          d.setFloorLoadCapacityKgSqm(industrial.floorLoadCapacityKgSqm());
          d.setPowerCapacityKva(industrial.powerCapacityKva());
          d.setHasThreePhasePower(
              industrial.hasThreePhasePower() != null ? industrial.hasThreePhasePower() : false);
          d.setHasCrane(industrial.hasCrane() != null ? industrial.hasCrane() : false);
          d.setCraneCapacityTons(industrial.craneCapacityTons());
          d.setHasHazmatCertification(
              industrial.hasHazmatCertification() != null
                  ? industrial.hasHazmatCertification()
                  : false);
          d.setHasVentilationSystem(
              industrial.hasVentilationSystem() != null
                  ? industrial.hasVentilationSystem()
                  : false);
          d.setHasClimateControl(
              industrial.hasClimateControl() != null ? industrial.hasClimateControl() : false);
          d.setYardAreaValue(industrial.yardAreaValue());
          d.setYardAreaUnit(industrial.yardAreaUnit());
          d.setZoningClassification(industrial.zoningClassification());
          d.setCreatedBy(userId);
          d.setUpdatedBy(userId);
          industrialDetailsRepository.save(d);
        }
      }
      case AGRICULTURAL -> {
        if (agricultural != null) {
          PropertyAgriculturalDetails d = new PropertyAgriculturalDetails();
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setTotalLandAreaValue(agricultural.totalLandAreaValue());
          d.setTotalLandAreaUnit(agricultural.totalLandAreaUnit());
          d.setArableAreaValue(agricultural.arableAreaValue());
          d.setArableAreaUnit(agricultural.arableAreaUnit());
          d.setSoilType(agricultural.soilType());
          d.setHasWaterRights(
              agricultural.hasWaterRights() != null ? agricultural.hasWaterRights() : false);
          d.setWaterSource(agricultural.waterSource());
          d.setIrrigationType(agricultural.irrigationType());
          d.setFencingType(agricultural.fencingType());
          d.setHasOutbuildings(
              agricultural.hasOutbuildings() != null ? agricultural.hasOutbuildings() : false);
          d.setOutbuildingDetails(agricultural.outbuildingDetails());
          d.setCurrentUse(agricultural.currentUse());
          d.setZoningClassification(agricultural.zoningClassification());
          d.setCreatedBy(userId);
          d.setUpdatedBy(userId);
          agriculturalDetailsRepository.save(d);
        }
      }
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
      ResidentialDetailsRequest residential,
      CommercialDetailsRequest commercial,
      IndustrialDetailsRequest industrial,
      AgriculturalDetailsRequest agricultural) {
    switch (category) {
      case RESIDENTIAL -> {
        if (residential != null) {
          Optional<PropertyResidentialDetails> existing =
              residentialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
          PropertyResidentialDetails d = existing.orElseGet(PropertyResidentialDetails::new);
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setBedrooms(residential.bedrooms());
          d.setBathrooms(residential.bathrooms());
          d.setFurnished(residential.furnished() != null ? residential.furnished() : false);
          d.setPetPolicy(residential.petPolicy());
          d.setUpdatedBy(userId);
          if (d.getId() == null) {
            d.setCreatedBy(userId);
          }
          residentialDetailsRepository.save(d);
        }
      }
      case COMMERCIAL -> {
        if (commercial != null) {
          Optional<PropertyCommercialDetails> existing =
              commercialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
          PropertyCommercialDetails d = existing.orElseGet(PropertyCommercialDetails::new);
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setUsableAreaValue(commercial.usableAreaValue());
          d.setUsableAreaUnit(commercial.usableAreaUnit());
          d.setCommonAreaValue(commercial.commonAreaValue());
          d.setCommonAreaUnit(commercial.commonAreaUnit());
          d.setFloorLevel(commercial.floorLevel());
          d.setCeilingHeightM(commercial.ceilingHeightM());
          d.setHasStorefront(
              commercial.hasStorefront() != null ? commercial.hasStorefront() : false);
          d.setHasSignageRights(
              commercial.hasSignageRights() != null ? commercial.hasSignageRights() : false);
          d.setZoningClassification(commercial.zoningClassification());
          d.setMaxOccupancy(commercial.maxOccupancy());
          d.setRestroomCount(commercial.restroomCount());
          d.setHasKitchenFacility(
              commercial.hasKitchenFacility() != null ? commercial.hasKitchenFacility() : false);
          d.setAccessibilityCompliant(
              commercial.accessibilityCompliant() != null
                  ? commercial.accessibilityCompliant()
                  : false);
          d.setUpdatedBy(userId);
          if (d.getId() == null) {
            d.setCreatedBy(userId);
          }
          commercialDetailsRepository.save(d);
        }
      }
      case INDUSTRIAL -> {
        if (industrial != null) {
          Optional<PropertyIndustrialDetails> existing =
              industrialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
          PropertyIndustrialDetails d = existing.orElseGet(PropertyIndustrialDetails::new);
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setClearHeightM(industrial.clearHeightM());
          d.setLoadingDocks(industrial.loadingDocks());
          d.setDriveInDoors(industrial.driveInDoors());
          d.setFloorLoadCapacityKgSqm(industrial.floorLoadCapacityKgSqm());
          d.setPowerCapacityKva(industrial.powerCapacityKva());
          d.setHasThreePhasePower(
              industrial.hasThreePhasePower() != null ? industrial.hasThreePhasePower() : false);
          d.setHasCrane(industrial.hasCrane() != null ? industrial.hasCrane() : false);
          d.setCraneCapacityTons(industrial.craneCapacityTons());
          d.setHasHazmatCertification(
              industrial.hasHazmatCertification() != null
                  ? industrial.hasHazmatCertification()
                  : false);
          d.setHasVentilationSystem(
              industrial.hasVentilationSystem() != null
                  ? industrial.hasVentilationSystem()
                  : false);
          d.setHasClimateControl(
              industrial.hasClimateControl() != null ? industrial.hasClimateControl() : false);
          d.setYardAreaValue(industrial.yardAreaValue());
          d.setYardAreaUnit(industrial.yardAreaUnit());
          d.setZoningClassification(industrial.zoningClassification());
          d.setUpdatedBy(userId);
          if (d.getId() == null) {
            d.setCreatedBy(userId);
          }
          industrialDetailsRepository.save(d);
        }
      }
      case AGRICULTURAL -> {
        if (agricultural != null) {
          Optional<PropertyAgriculturalDetails> existing =
              agriculturalDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
          PropertyAgriculturalDetails d = existing.orElseGet(PropertyAgriculturalDetails::new);
          d.setPropertyId(propertyId);
          d.setTeamId(teamId);
          d.setTotalLandAreaValue(agricultural.totalLandAreaValue());
          d.setTotalLandAreaUnit(agricultural.totalLandAreaUnit());
          d.setArableAreaValue(agricultural.arableAreaValue());
          d.setArableAreaUnit(agricultural.arableAreaUnit());
          d.setSoilType(agricultural.soilType());
          d.setHasWaterRights(
              agricultural.hasWaterRights() != null ? agricultural.hasWaterRights() : false);
          d.setWaterSource(agricultural.waterSource());
          d.setIrrigationType(agricultural.irrigationType());
          d.setFencingType(agricultural.fencingType());
          d.setHasOutbuildings(
              agricultural.hasOutbuildings() != null ? agricultural.hasOutbuildings() : false);
          d.setOutbuildingDetails(agricultural.outbuildingDetails());
          d.setCurrentUse(agricultural.currentUse());
          d.setZoningClassification(agricultural.zoningClassification());
          d.setUpdatedBy(userId);
          if (d.getId() == null) {
            d.setCreatedBy(userId);
          }
          agriculturalDetailsRepository.save(d);
        }
      }
      case MIXED_USE -> {
        // No detail table
      }
    }
  }

  private ResidentialDetailsResponse buildResidentialResponse(UUID propertyId, UUID teamId) {
    return residentialDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new ResidentialDetailsResponse(
                    d.getBedrooms(), d.getBathrooms(), d.getFurnished(), d.getPetPolicy()))
        .orElse(null);
  }

  private CommercialDetailsResponse buildCommercialResponse(UUID propertyId, UUID teamId) {
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
                    d.getCeilingHeightM(),
                    d.getHasStorefront(),
                    d.getHasSignageRights(),
                    d.getZoningClassification(),
                    d.getMaxOccupancy(),
                    d.getRestroomCount(),
                    d.getHasKitchenFacility(),
                    d.getAccessibilityCompliant()))
        .orElse(null);
  }

  private IndustrialDetailsResponse buildIndustrialResponse(UUID propertyId, UUID teamId) {
    return industrialDetailsRepository
        .findByPropertyIdAndTeamId(propertyId, teamId)
        .map(
            d ->
                new IndustrialDetailsResponse(
                    d.getClearHeightM(),
                    d.getLoadingDocks(),
                    d.getDriveInDoors(),
                    d.getFloorLoadCapacityKgSqm(),
                    d.getPowerCapacityKva(),
                    d.getHasThreePhasePower(),
                    d.getHasCrane(),
                    d.getCraneCapacityTons(),
                    d.getHasHazmatCertification(),
                    d.getHasVentilationSystem(),
                    d.getHasClimateControl(),
                    d.getYardAreaValue(),
                    d.getYardAreaUnit(),
                    d.getZoningClassification()))
        .orElse(null);
  }

  private AgriculturalDetailsResponse buildAgriculturalResponse(UUID propertyId, UUID teamId) {
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
                    d.getZoningClassification()))
        .orElse(null);
  }

  private PropertyResponse toResponseWithMainPhoto(
      Property property, UUID teamId, boolean includeNestedCollections) {
    PropertyResponse response = propertyMapper.toResponse(property);

    List<Photo> photos =
        photoRepository.findByEntityAndTeamId("PROPERTY", property.getId(), teamId);

    Optional<Photo> mainPhoto =
        photos.stream().filter(photo -> Boolean.TRUE.equals(photo.getIsMainPhoto())).findFirst();

    String mainPhotoUrl =
        mainPhoto
            .map(photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString())
            .orElse(null);

    String mainPhotoThumbnailUrl =
        mainPhoto
            .map(
                photo -> {
                  String key =
                      photo.getThumbnailFileKey() != null
                          ? photo.getThumbnailFileKey()
                          : photo.getFileKey();
                  return s3StorageService.generatePresignedUrl(key).toString();
                })
            .orElse(null);

    List<PropertyOutdoorAreaResponse> outdoorAreas =
        includeNestedCollections
            ? outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
                .map(
                    a ->
                        new PropertyOutdoorAreaResponse(
                            a.getIdentifier(),
                            a.getType(),
                            a.getAreaValue(),
                            a.getAreaUnit(),
                            a.getCreatedAt(),
                            a.getUpdatedAt()))
                .toList()
            : null;

    List<PropertyAmenityResponse> amenities =
        includeNestedCollections
            ? propertyAmenityService.buildPropertyAmenityResponses(property.getId(), teamId)
            : null;

    // Build category-specific detail responses
    ResidentialDetailsResponse residentialDetails = null;
    CommercialDetailsResponse commercialDetails = null;
    IndustrialDetailsResponse industrialDetails = null;
    AgriculturalDetailsResponse agriculturalDetails = null;

    if (property.getPropertyCategory() != null) {
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
        // Investment & Financial
        response.purchasePrice(),
        response.purchasePriceCurrency(),
        response.purchaseDate(),
        response.currentMarketValue(),
        response.currentMarketValueCurrency(),
        response.marketValueDate(),
        response.mortgageType(),
        response.mortgageAmount(),
        response.mortgageAmountCurrency(),
        response.mortgageInterestRate(),
        response.mortgageStartDate(),
        response.mortgageEndDate(),
        response.monthlyMortgagePayment(),
        response.monthlyMortgagePaymentCurrency(),
        response.annualPropertyTax(),
        response.annualPropertyTaxCurrency(),
        response.annualInsurance(),
        response.annualInsuranceCurrency(),
        response.annualHoaFee(),
        response.annualHoaFeeCurrency(),
        response.annualManagementFee(),
        response.annualManagementFeeCurrency(),
        response.annualMaintenanceReserve(),
        response.annualMaintenanceReserveCurrency(),
        response.annualPropertyTaxDueMonth(),
        response.annualInsuranceDueMonth(),
        response.annualHoaFeeDueMonth(),
        response.annualManagementFeeDueMonth(),
        response.annualMaintenanceReserveDueMonth(),
        response.depreciationMethod(),
        response.depreciationYears(),
        response.landValue(),
        response.landValueCurrency(),
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
