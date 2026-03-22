package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactType {
  INDIVIDUAL("Individual"),
  COMPANY("Company"),
  SERVICE_PROVIDER("Service Provider");

  private final String displayName;
}
