package com.buurman.domain;

/** Whether, and in what form, a lease agreement document can be produced for a contract. */
public enum LeaseAvailability {
  AVAILABLE_DOCUMENT,
  AVAILABLE_EXAMPLE_TEXT,
  UNAVAILABLE_COUNTRY,
  UNAVAILABLE_NO_COUNTRY;

  public boolean isAvailable() {
    return this == AVAILABLE_DOCUMENT || this == AVAILABLE_EXAMPLE_TEXT;
  }
}
