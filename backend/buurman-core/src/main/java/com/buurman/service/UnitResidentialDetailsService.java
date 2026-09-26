package com.buurman.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitType;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.UpdateUnitResidentialDetailsRequest;
import com.buurman.dto.response.UnitResidentialDetailsResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Manages the 1:1 {@link UnitResidentialDetails} row attached to an APARTMENT {@link Unit}.
 *
 * <p>Kept separate from {@link UnitService}, mirroring how the pre-BUUR-106 property-level
 * equivalent lived in its own {@code PropertyAmenityService}-style class rather than on the main
 * entity service — residential details are a small, self-contained concern with their own
 * visibility rule below.
 *
 * <p>Rows for non-APARTMENT units are never surfaced or written here, but are deliberately never
 * deleted either: a landlord who mistypes a unit's type (e.g. APARTMENT vs STORAGE) and corrects it
 * later must get their bedroom/bathroom data back. {@link #getResidentialDetails} returns an empty
 * response instead of the stored row while the type isn't APARTMENT, and {@link
 * #updateResidentialDetails} refuses writes outright so a landlord can't unknowingly edit a field
 * they can't see.
 */
@Service
@RequiredArgsConstructor
public class UnitResidentialDetailsService {

  private static final UnitResidentialDetailsResponse EMPTY =
      new UnitResidentialDetailsResponse(null, null, false, null);

  private final UnitRepository unitRepository;
  private final UnitResidentialDetailsRepository unitResidentialDetailsRepository;

  public UnitResidentialDetailsResponse getResidentialDetails(
      UnitIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (unit.getUnitType() != UnitType.APARTMENT) {
      return EMPTY;
    }
    return unitResidentialDetailsRepository
        .findByUnitIdAndTeamId(unit.getId(), teamId)
        .map(UnitResidentialDetailsService::toResponse)
        .orElse(EMPTY);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public UnitResidentialDetailsResponse updateResidentialDetails(
      UnitIdentifier identifier,
      UpdateUnitResidentialDetailsRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (unit.getUnitType() != UnitType.APARTMENT) {
      throw new BusinessRuleException(
          "Residential details can only be set on APARTMENT units. Change the unit type first.");
    }

    UnitResidentialDetails details =
        unitResidentialDetailsRepository
            .findByUnitIdAndTeamId(unit.getId(), teamId)
            .orElseGet(
                () -> {
                  UnitResidentialDetails created = new UnitResidentialDetails();
                  created.setUnitId(unit.getId());
                  created.setTeamId(teamId);
                  created.setCreatedBy(Optional.of(principal.getUserId()));
                  return created;
                });

    details.setBedrooms(request.bedrooms());
    details.setBathrooms(request.bathrooms());
    details.setFurnished(request.furnished());
    details.setPetPolicy(request.petPolicy());
    details.setUpdatedBy(Optional.of(principal.getUserId()));

    UnitResidentialDetails saved = unitResidentialDetailsRepository.save(details);
    return toResponse(saved);
  }

  private static UnitResidentialDetailsResponse toResponse(UnitResidentialDetails details) {
    return new UnitResidentialDetailsResponse(
        details.getBedrooms().orElse(null),
        details.getBathrooms().orElse(null),
        details.isFurnished(),
        details.getPetPolicy().orElse(null));
  }
}
