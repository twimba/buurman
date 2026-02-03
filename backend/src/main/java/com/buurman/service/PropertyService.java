package com.buurman.service;

import com.buurman.domain.Document;
import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final AuditService auditService;
    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyMapper propertyMapper,
            AuditService auditService,
            DocumentRepository documentRepository,
            S3StorageService s3StorageService) {
        this.propertyRepository = propertyRepository;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
        this.documentRepository = documentRepository;
        this.s3StorageService = s3StorageService;
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse createProperty(CreatePropertyRequest request, UserPrincipal principal) {
        Property property = propertyMapper.toEntity(request);
        property.setIdentifier(UlidGenerator.generate());
        property.setTeamId(principal.getTeamId());
        property.setCreatedBy(principal.getUserId());
        property.setUpdatedBy(principal.getUserId());

        Property savedProperty = propertyRepository.save(property);
        log.info("Property created: {} for team {}", savedProperty.getId(), principal.getTeamId());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
                "PROPERTY",
                savedProperty.getId(),
                principal.getUserId(),
                savedProperty
        );

        return toResponseWithMainPhoto(savedProperty, principal.getTeamId());
    }

    public List<PropertyResponse> getProperties(UserPrincipal principal, Property.PropertyStatus status) {
        List<Property> properties;
        if (status != null) {
            properties = propertyRepository.findByTeamIdAndStatus(principal.getTeamId(), status);
        } else {
            properties = propertyRepository.findAllByTeamId(principal.getTeamId());
        }

        return properties.stream()
                .map(property -> toResponseWithMainPhoto(property, principal.getTeamId()))
                .toList();
    }

    public PropertyResponse getProperty(UUID propertyId, UserPrincipal principal) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        return toResponseWithMainPhoto(property, principal.getTeamId());
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse updateProperty(
            UUID propertyId,
            UpdatePropertyRequest request,
            UserPrincipal principal) {

        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Create a simple representation of the old state for audit (just store the request data)
        PropertyResponse oldState = toResponseWithMainPhoto(property, principal.getTeamId());

        propertyMapper.updateEntity(property, request);
        property.setUpdatedBy(principal.getUserId());

        Property updatedProperty = propertyRepository.save(property);
        PropertyResponse newState = toResponseWithMainPhoto(updatedProperty, principal.getTeamId());

        log.info("Property updated: {} for team {}", propertyId, principal.getTeamId());

        // Log to audit trail
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

    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteProperty(UUID propertyId, UserPrincipal principal) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        propertyRepository.softDeleteByIdAndTeamId(propertyId, principal.getTeamId());
        log.info("Property deleted: {} for team {}", propertyId, principal.getTeamId());

        // Log to audit trail
        auditService.logDelete(
                principal.getTeamId(),
                "PROPERTY",
                propertyId,
                principal.getUserId(),
                property
        );
    }

    private PropertyResponse toResponseWithMainPhoto(Property property, UUID teamId) {
        PropertyResponse response = propertyMapper.toResponse(property);

        // Find main photo for this property
        List<Document> photos = documentRepository.findByEntityAndTeamIdAndCategory(
                "PROPERTY",
                property.getId(),
                teamId,
                Document.Category.PHOTO.name()
        );

        Optional<Document> mainPhoto = photos.stream()
                .filter(photo -> Boolean.TRUE.equals(photo.getIsMainPhoto()))
                .findFirst();

        String mainPhotoUrl = mainPhoto
                .map(photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString())
                .orElse(null);

        return new PropertyResponse(
                response.id(),
                response.identifier(),
                response.teamId(),
                response.street(),
                response.city(),
                response.postalCode(),
                response.country(),
                response.latitude(),
                response.longitude(),
                response.bedrooms(),
                response.bathrooms(),
                response.squareMeters(),
                response.propertyType(),
                response.status(),
                mainPhotoUrl,
                response.createdAt(),
                response.updatedAt()
        );
    }
}
