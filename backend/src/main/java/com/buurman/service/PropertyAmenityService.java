package com.buurman.service;

import com.buurman.domain.Amenity;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyAmenity;
import com.buurman.dto.request.PropertyAmenityRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.repository.AmenityRepository;
import com.buurman.repository.PropertyAmenityRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PropertyAmenityService {

    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyRepository propertyRepository;

    public PropertyAmenityService(
            AmenityRepository amenityRepository,
            PropertyAmenityRepository propertyAmenityRepository,
            PropertyRepository propertyRepository) {
        this.amenityRepository = amenityRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
        this.propertyRepository = propertyRepository;
    }

    public Map<String, List<AmenityResponse>> getAllAmenitiesGrouped() {
        return amenityRepository.findAll().stream()
                .map(a -> new AmenityResponse(a.getIdentifier(), a.getName(), a.getCategory(), a.getIcon()))
                .collect(Collectors.groupingBy(AmenityResponse::category));
    }

    public List<PropertyAmenityResponse> getPropertyAmenities(String propertyIdentifier, UserPrincipal principal) {
        Property property = resolveProperty(propertyIdentifier, principal);
        return buildPropertyAmenityResponses(property.getId(), principal.getTeamId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyAmenityResponse addAmenity(
            String propertyIdentifier,
            PropertyAmenityRequest request,
            UserPrincipal principal) {

        Property property = resolveProperty(propertyIdentifier, principal);
        Amenity amenity = amenityRepository.findByIdentifier(request.amenityIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found: " + request.amenityIdentifier()));

        propertyAmenityRepository.findByPropertyIdAndAmenityIdAndTeamId(
                property.getId(), amenity.getId(), principal.getTeamId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Amenity already linked to this property");
                });

        PropertyAmenity pa = new PropertyAmenity();
        pa.setPropertyId(property.getId());
        pa.setAmenityId(amenity.getId());
        pa.setTeamId(principal.getTeamId());
        pa.setNotes(request.notes());
        pa.setCreatedBy(principal.getUserId());
        pa.setUpdatedBy(principal.getUserId());

        propertyAmenityRepository.save(pa);

        return toPropertyAmenityResponse(amenity, pa);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void removeAmenity(
            String propertyIdentifier,
            String amenityIdentifier,
            UserPrincipal principal) {

        Property property = resolveProperty(propertyIdentifier, principal);
        Amenity amenity = amenityRepository.findByIdentifier(amenityIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found: " + amenityIdentifier));

        PropertyAmenity pa = propertyAmenityRepository.findByPropertyIdAndAmenityIdAndTeamId(
                        property.getId(), amenity.getId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Amenity not linked to this property"));

        propertyAmenityRepository.softDeleteByIdAndTeamId(pa.getId(), principal.getTeamId());
    }

    public List<PropertyAmenityResponse> buildPropertyAmenityResponses(UUID propertyId, UUID teamId) {
        List<PropertyAmenity> links = propertyAmenityRepository.findByPropertyIdAndTeamId(propertyId, teamId);
        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, Amenity> amenityMap = amenityRepository.findAll().stream()
                .collect(Collectors.toMap(Amenity::getId, Function.identity()));

        return links.stream()
                .map(pa -> {
                    Amenity amenity = amenityMap.get(pa.getAmenityId());
                    if (amenity == null) return null;
                    return toPropertyAmenityResponse(amenity, pa);
                })
                .filter(r -> r != null)
                .toList();
    }

    private PropertyAmenityResponse toPropertyAmenityResponse(Amenity amenity, PropertyAmenity pa) {
        return new PropertyAmenityResponse(
                amenity.getIdentifier(),
                amenity.getName(),
                amenity.getCategory(),
                amenity.getIcon(),
                pa.getNotes()
        );
    }

    private Property resolveProperty(String propertyIdentifier, UserPrincipal principal) {
        return propertyRepository.findByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }
}
