package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactTag {
  VIP("VIP"),
  PROSPECT("Prospect"),
  LATE_PAYER("Late Payer"),
  LONG_TERM("Long-term"),
  KEY_HOLDER("Key Holder"),
  DO_NOT_CONTACT("Do Not Contact"),
  FORMER_TENANT("Former Tenant"),
  REFERRED("Referred");

  private final String displayName;
}
