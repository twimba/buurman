package com.buurman.service;

import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final AuditService auditService;

    public PropertyService(PropertyRepository propertyRepository, PropertyMapper propertyMapper, AuditService auditService) {
        this.propertyRepository = propertyRepository;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
    }

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

        return propertyMapper.toResponse(savedProperty);
    }

    public List<PropertyResponse> getProperties(UserPrincipal principal, Property.PropertyStatus status) {
        List<Property> properties;
        if (status != null) {
            properties = propertyRepository.findByTeamIdAndStatus(principal.getTeamId(), status);
        } else {
            properties = propertyRepository.findAllByTeamId(principal.getTeamId());
        }

        return properties.stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    public PropertyResponse getProperty(UUID propertyId, UserPrincipal principal) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        return propertyMapper.toResponse(property);
    }

    public PropertyResponse updateProperty(
            UUID propertyId,
            UpdatePropertyRequest request,
            UserPrincipal principal) {

        Property property = propertyRepository.findByIdAndTeamId(propertyId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Create a simple representation of the old state for audit (just store the request data)
        PropertyResponse oldState = propertyMapper.toResponse(property);

        propertyMapper.updateEntity(property, request);
        property.setUpdatedBy(principal.getUserId());

        Property updatedProperty = propertyRepository.save(property);
        PropertyResponse newState = propertyMapper.toResponse(updatedProperty);

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
}
