package com.buurman.domain;

import lombok.Getter;

@Getter
public enum DataImportEntityType {
  CONTACT("Contact");

  private final String displayName;

  DataImportEntityType(String displayName) {
    this.displayName = displayName;
  }
}
