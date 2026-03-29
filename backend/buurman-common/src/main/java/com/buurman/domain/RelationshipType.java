package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RelationshipType {
  GUARANTOR_FOR("Guarantor for"),
  FAMILY_OF("Family of"),
  PARTNER_OF("Partner of"),
  WORKS_FOR("Works for"),
  CONTACT_PERSON_FOR("Contact person for"),
  OTHER("Other");

  private final String displayName;

  public RelationshipType inverse() {
    return switch (this) {
      case GUARANTOR_FOR -> GUARANTOR_FOR;
      case FAMILY_OF -> FAMILY_OF;
      case PARTNER_OF -> PARTNER_OF;
      case WORKS_FOR -> WORKS_FOR;
      case CONTACT_PERSON_FOR -> CONTACT_PERSON_FOR;
      case OTHER -> OTHER;
    };
  }

  public String inverseDisplayName() {
    return switch (this) {
      case GUARANTOR_FOR -> "Guaranteed by";
      case FAMILY_OF -> "Family of";
      case PARTNER_OF -> "Partner of";
      case WORKS_FOR -> "Employer of";
      case CONTACT_PERSON_FOR -> "Has contact person";
      case OTHER -> "Other";
    };
  }

  public boolean isSymmetric() {
    return this == FAMILY_OF || this == PARTNER_OF;
  }
}
