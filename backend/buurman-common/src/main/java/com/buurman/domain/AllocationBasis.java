package com.buurman.domain;

/** How a building-level expense is divided across a property's units. */
public enum AllocationBasis {
  /** Proportional to each unit's area. Falls back to EQUAL when no unit has an area. */
  AREA,
  /** Equal split across all units. */
  EQUAL,
  /** Proportional to each unit's allocation_share percentage. */
  CUSTOM,
  /** Caller-supplied per-unit amounts. */
  MANUAL
}
