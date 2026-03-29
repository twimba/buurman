package com.buurman.domain;

import lombok.Getter;

@Getter
public enum DataImportStatus {
  PROCESSING("Processing"),
  COMPLETED("Completed"),
  PARTIALLY_COMPLETED("Partially Completed"),
  FAILED("Failed"),
  REVERTED("Reverted");

  private final String displayName;

  DataImportStatus(String displayName) {
    this.displayName = displayName;
  }
}
