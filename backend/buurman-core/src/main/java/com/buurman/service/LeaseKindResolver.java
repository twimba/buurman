package com.buurman.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.LeaseRegime;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.repository.UnitResidentialDetailsRepository;

/** Derives which lease document family applies to a contract. */
@Component
public class LeaseKindResolver {

  private final UnitResidentialDetailsRepository unitDetailsRepository;

  public LeaseKindResolver(UnitResidentialDetailsRepository unitDetailsRepository) {
    this.unitDetailsRepository = unitDetailsRepository;
  }

  /** Loads the unit's residential details (if the contract has a unit) and derives the kind. */
  public LeaseKind resolveFor(Contract contract, Property property, UUID teamId) {
    Optional<UnitResidentialDetails> unitDetails =
        Optional.ofNullable(contract.getUnitId())
            .flatMap(unitId -> unitDetailsRepository.findByUnitIdAndTeamId(unitId, teamId));
    return resolve(contract, property, unitDetails);
  }

  public LeaseKind resolve(
      Contract contract, Property property, Optional<UnitResidentialDetails> unitDetails) {
    LeaseRegime regime = contract.getLeaseRegime();
    if (regime == LeaseRegime.SHORT_TERM) {
      return LeaseKind.SHORT_TERM;
    }
    if (regime == LeaseRegime.STUDENT_OR_MOBILITY) {
      return LeaseKind.STUDENT_MOBILITY;
    }

    PropertyCategory category =
        Optional.ofNullable(property.getPropertyCategory()).orElse(PropertyCategory.RESIDENTIAL);
    return switch (category) {
      case RESIDENTIAL ->
          unitDetails.map(UnitResidentialDetails::isFurnished).orElse(false)
              ? LeaseKind.RESIDENTIAL_FURNISHED
              : LeaseKind.RESIDENTIAL;
      case COMMERCIAL, INDUSTRIAL -> LeaseKind.COMMERCIAL;
      case MIXED_USE -> LeaseKind.MIXED_USE;
      case AGRICULTURAL -> LeaseKind.AGRICULTURAL;
    };
  }
}
