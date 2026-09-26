package com.buurman.domain;

/** Occupancy state of a single unit. Moved verbatim from the former Property.PropertyStatus. */
public enum UnitStatus {
  VACANT,
  OCCUPIED,
  SELF_OCCUPIED,
  MAINTENANCE,
  UNAVAILABLE,
  UNDER_RENOVATION,
  FALLOW,
  LISTED
}
