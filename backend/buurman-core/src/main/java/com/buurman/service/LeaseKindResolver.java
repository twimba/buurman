package com.buurman.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.UnitResidentialDetails;

/** Derives which lease document family applies to a contract. */
@Component
public class LeaseKindResolver {

  public LeaseKind resolve(
      Contract contract, Property property, Optional<UnitResidentialDetails> unitDetails) {
    switch (contract.getLeaseRegime()) {
      case SHORT_TERM:
        return LeaseKind.SHORT_TERM;
      case STUDENT_OR_MOBILITY:
        return LeaseKind.STUDENT_MOBILITY;
      default:
        break;
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
