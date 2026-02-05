package com.buurman.service;

import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.repository.PropertyOutdoorAreaRepository;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final AuditService auditService;
    private final DocumentService documentService;
    private final PhotoService photoService;
    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final PropertyOutdoorAreaRepository outdoorAreaRepository;
    private final PropertyAmenityService propertyAmenityService;
    private final MetricsService metricsService;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyMapper propertyMapper,
            AuditService auditService,
            DocumentService documentService,
            PhotoService photoService,
            PhotoRepository photoRepository,
            S3StorageService s3StorageService,
            PropertyOutdoorAreaRepository outdoorAreaRepository,
            PropertyAmenityService propertyAmenityService,
            MetricsService metricsService) {
        this.propertyRepository = propertyRepository;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
        this.documentService = documentService;
        this.photoService = photoService;
        this.photoRepository = photoRepository;
        this.s3StorageService = s3StorageService;
        this.outdoorAreaRepository = outdoorAreaRepository;
        this.propertyAmenityService = propertyAmenityService;
        this.metricsService = metricsService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse createProperty(CreatePropertyRequest request, UserPrincipal principal) {
        Property property = propertyMapper.toEntity(request);
        property.setIdentifier(UlidGenerator.generate(EntityPrefix.PRO));
        property.setTeamId(principal.getTeamId());
        property.setCreatedBy(principal.getUserId());
        property.setUpdatedBy(principal.getUserId());

        Property savedProperty = propertyRepository.save(property);

        metricsService.incrementCounter("property.created.total");

        log.info("Property created: {} for team {}", savedProperty.getIdentifier(), principal.getTeamId());

        auditService.logCreate(
                principal.getTeamId(),
                "PROPERTY",
                savedProperty.getId(),
                principal.getUserId(),
                savedProperty
        );

        return toResponseWithMainPhoto(savedProperty, principal.getTeamId(), true);
    }

    public List<PropertyResponse> getProperties(UserPrincipal principal, Property.PropertyStatus status) {
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

    public PageResponse<PropertyResponse> getPropertiesPaginated(UserPrincipal principal, String status, PageRequest pageRequest) {
        PaginatedResult<Property> result = propertyRepository.findAllByTeamIdPaginated(
                principal.getTeamId(), status, pageRequest);
        List<PropertyResponse> responses = result.items().stream()
                .map(property -> toResponseWithMainPhoto(property, principal.getTeamId(), false))
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    public PropertyResponse getProperty(String identifier, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        return toResponseWithMainPhoto(property, principal.getTeamId(), true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse updateProperty(
            String identifier,
            UpdatePropertyRequest request,
            UserPrincipal principal) {

        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        PropertyResponse oldState = toResponseWithMainPhoto(property, principal.getTeamId(), true);

        propertyMapper.updateEntity(property, request);
        property.setUpdatedBy(principal.getUserId());

        Property updatedProperty = propertyRepository.save(property);
        PropertyResponse newState = toResponseWithMainPhoto(updatedProperty, principal.getTeamId(), true);

        log.info("Property updated: {} for team {}", identifier, principal.getTeamId());

        auditService.logUpdate(
                principal.getTeamId(),
                "PROPERTY",
                updatedProperty.getId(),
                principal.getUserId(),
                oldState,
                newState,
                auditService.getChangedFields(oldState, newState)
        );

        return newState;
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteProperty(String identifier, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        propertyRepository.softDeleteByIdAndTeamId(property.getId(), principal.getTeamId());
        log.info("Property deleted: {} for team {}", identifier, principal.getTeamId());

        auditService.logDelete(
                principal.getTeamId(),
                "PROPERTY",
                property.getId(),
                principal.getUserId(),
                property
        );
    }

    public DocumentResponse uploadDocument(String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        return documentService.uploadDocument(file, "PROPERTY", property.getId(), title, notes, principal);
    }

    public List<DocumentResponse> getDocuments(String identifier, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
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
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        return auditService.getEntityAuditLog(principal.getTeamId(), "PROPERTY", property.getId());
    }

    public List<PhotoResponse> getPhotos(String identifier, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        return photoService.getPhotos("PROPERTY", property.getId(), principal);
    }

    public PhotoResponse uploadPhoto(String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        return photoService.uploadPhoto(file, "PROPERTY", property.getId(), title, notes, principal);
    }

    public PhotoResponse setMainPhoto(String identifier, String photoIdentifier, UserPrincipal principal) {
        Property property = propertyRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        Photo photo = photoRepository.findByIdentifierAndTeamId(photoIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        return photoService.setMainPhoto(photo.getId(), "PROPERTY", property.getId(), principal);
    }

    private PropertyResponse toResponseWithMainPhoto(Property property, UUID teamId, boolean includeNestedCollections) {
        PropertyResponse response = propertyMapper.toResponse(property);

        List<Photo> photos = photoRepository.findByEntityAndTeamId(
                "PROPERTY",
                property.getId(),
                teamId
        );

        Optional<Photo> mainPhoto = photos.stream()
                .filter(photo -> Boolean.TRUE.equals(photo.getIsMainPhoto()))
                .findFirst();

        String mainPhotoUrl = mainPhoto
                .map(photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString())
                .orElse(null);

        List<PropertyOutdoorAreaResponse> outdoorAreas = includeNestedCollections
                ? outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId)
                        .stream()
                        .map(a -> new PropertyOutdoorAreaResponse(
                                a.getIdentifier(), a.getType(), a.getAreaValue(),
                                a.getAreaUnit(), a.getCreatedAt(), a.getUpdatedAt()))
                        .toList()
                : null;

        List<PropertyAmenityResponse> amenities = includeNestedCollections
                ? propertyAmenityService.buildPropertyAmenityResponses(property.getId(), teamId)
                : null;

        return new PropertyResponse(
                response.identifier(),
                response.street(),
                response.city(),
                response.postalCode(),
                response.country(),
                response.latitude(),
                response.longitude(),
                response.bedrooms(),
                response.bathrooms(),
                response.areaValue(),
                response.areaUnit(),
                response.propertyType(),
                response.status(),
                mainPhotoUrl,
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
                // Nested collections (null on list endpoint, populated on detail)
                outdoorAreas,
                amenities,
                response.createdAt(),
                response.updatedAt()
        );
    }
}
