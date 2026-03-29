package com.buurman.domain;

import lombok.Getter;

@Getter
public enum RentComponentType {
  BASE_RENT("Base Rent"),
  UTILITIES_ADVANCE("Utilities Advance"),
  SERVICE_COSTS("Service Costs"),
  HOA_FEES("HOA / Condo Fees"),
  FURNITURE_RENTAL("Furniture Rental"),
  PARKING("Parking"),
  STORAGE("Storage"),
  GARBAGE_COLLECTION("Garbage Collection"),
  OTHER("Other");

  private final String displayName;

  RentComponentType(String displayName) {
    this.displayName = displayName;
  }
}
