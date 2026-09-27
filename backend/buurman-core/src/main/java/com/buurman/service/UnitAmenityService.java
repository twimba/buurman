package com.buurman.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Amenity;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitAmenity;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.UpdateUnitAmenitiesRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.repository.AmenityRepository;
import com.buurman.repository.UnitAmenityRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Manages the amenity links attached to a {@link Unit}. Kept separate from {@link UnitService},
 * mirroring how the pre-BUUR-106 property-level equivalent ({@code PropertyAmenityService}) stayed
 * separate from {@code PropertyService} — amenity linking is its own small, self-contained concern.
 */
@Service
@RequiredArgsConstructor
public class UnitAmenityService {

  private final UnitRepository unitRepository;
  private final UnitAmenityRepository unitAmenityRepository;
  private final AmenityRepository amenityRepository;

  public List<AmenityResponse> getAmenities(UnitIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);
    return toResponses(unitAmenityRepository.findAmenitiesByUnitIdAndTeamId(unit.getId(), teamId));
  }

  /**
   * Replaces the unit's full amenity set in one call: links not in the request are soft-deleted,
   * links already active stay untouched, and requested links are inserted or — since {@code
   * uq_unit_amenities} is not a partial index — reactivated in place if a soft-deleted row for that
   * amenity already exists.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<AmenityResponse> replaceAmenities(
      UnitIdentifier identifier, UpdateUnitAmenitiesRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID actorId = principal.getUserId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);

    Map<UUID, Amenity> desiredAmenitiesById =
        request.amenityIdentifiers().stream()
            .map(amenityRepository::getByIdentifier)
            .collect(
                Collectors.toMap(Amenity::getId, Function.identity(), (first, second) -> first));

    Map<UUID, UnitAmenity> existingLinksByAmenityId =
        unitAmenityRepository.findAllLinksByUnitIdAndTeamId(unit.getId(), teamId).stream()
            .collect(Collectors.toMap(UnitAmenity::getAmenityId, Function.identity()));

    desiredAmenitiesById
        .keySet()
        .forEach(
            amenityId -> {
              UnitAmenity existing = existingLinksByAmenityId.get(amenityId);
              if (existing == null) {
                unitAmenityRepository.insertLink(unit.getId(), amenityId, teamId, actorId);
              } else if (existing.getDeletedAt().isPresent()) {
                unitAmenityRepository.reactivateLink(existing.getId(), teamId, actorId);
              }
            });

    existingLinksByAmenityId.forEach(
        (amenityId, link) -> {
          if (!desiredAmenitiesById.containsKey(amenityId) && link.getDeletedAt().isEmpty()) {
            unitAmenityRepository.softDeleteLink(link.getId(), teamId, actorId);
          }
        });

    return toResponses(unitAmenityRepository.findAmenitiesByUnitIdAndTeamId(unit.getId(), teamId));
  }

  private List<AmenityResponse> toResponses(List<Amenity> amenities) {
    return amenities.stream()
        .map(
            a ->
                new AmenityResponse(
                    a.getIdentifier().orElseThrow(),
                    a.getName(),
                    a.getCategory(),
                    a.getIcon(),
                    a.getApplicableCategories()))
        .toList();
  }
}
