package com.buurman.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Amenity;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyAmenity;
import com.buurman.dto.request.PropertyAmenityRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.AmenityRepository;
import com.buurman.repository.PropertyAmenityRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyAmenityService {

  private final AmenityRepository amenityRepository;
  private final PropertyAmenityRepository propertyAmenityRepository;
  private final PropertyRepository propertyRepository;

  public Map<String, List<AmenityResponse>> getAllAmenitiesGrouped(
      @Nullable String propertyCategory) {
    List<Amenity> amenities;
    if (propertyCategory != null && !propertyCategory.isBlank()) {
      amenities = amenityRepository.findByApplicableCategory(propertyCategory);
    } else {
      amenities = amenityRepository.findAll();
    }
    return amenities.stream()
        .map(
            a ->
                new AmenityResponse(
                    a.getIdentifier(),
                    a.getName(),
                    a.getCategory(),
                    a.getIcon(),
                    a.getApplicableCategories()))
        .collect(groupingBy(AmenityResponse::category));
  }

  public List<PropertyAmenityResponse> getPropertyAmenities(
      String propertyIdentifier, UserPrincipal principal) {
    Property property = resolveProperty(propertyIdentifier, principal);
    return buildPropertyAmenityResponses(property.getId(), principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyAmenityResponse addAmenity(
      String propertyIdentifier, PropertyAmenityRequest request, UserPrincipal principal) {

    Property property = resolveProperty(propertyIdentifier, principal);
    Amenity amenity = amenityRepository.getByIdentifier(request.amenityIdentifier());

    propertyAmenityRepository
        .findByPropertyIdAndAmenityIdAndTeamId(
            property.getId(), amenity.getId(), principal.requireTeamId())
        .ifPresent(
            existing -> {
              throw new BusinessRuleException("Amenity already linked to this property");
            });

    PropertyAmenity pa = new PropertyAmenity();
    pa.setPropertyId(property.getId());
    pa.setAmenityId(amenity.getId());
    pa.setTeamId(principal.requireTeamId());
    pa.setNotes(request.notes());
    pa.setCreatedBy(principal.getUserId());
    pa.setUpdatedBy(principal.getUserId());

    propertyAmenityRepository.save(pa);

    return toPropertyAmenityResponse(amenity, pa);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void removeAmenity(
      String propertyIdentifier, String amenityIdentifier, UserPrincipal principal) {

    Property property = resolveProperty(propertyIdentifier, principal);
    Amenity amenity = amenityRepository.getByIdentifier(amenityIdentifier);

    PropertyAmenity pa =
        propertyAmenityRepository.getByPropertyIdAndAmenityIdAndTeamId(
            property.getId(), amenity.getId(), principal.requireTeamId());

    propertyAmenityRepository.softDeleteByIdAndTeamId(pa.getId(), principal.requireTeamId());
  }

  public List<PropertyAmenityResponse> buildPropertyAmenityResponses(UUID propertyId, UUID teamId) {
    List<PropertyAmenity> links =
        propertyAmenityRepository.findByPropertyIdAndTeamId(propertyId, teamId);
    if (links.isEmpty()) {
      return List.of();
    }

    Map<UUID, Amenity> amenityMap =
        amenityRepository.findAll().stream().collect(toMap(Amenity::getId, identity()));

    return links.stream()
        .map(
            pa -> {
              @Nullable Amenity amenity = amenityMap.get(pa.getAmenityId());
              if (amenity == null) {
                return null;
              }
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
        Optional.ofNullable(amenity.getIcon()),
        Optional.ofNullable(pa.getNotes()));
  }

  private Property resolveProperty(String propertyIdentifier, UserPrincipal principal) {
    return propertyRepository.getByIdentifierAndTeamId(
        propertyIdentifier, principal.requireTeamId());
  }
}
