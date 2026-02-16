package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractPartyRole {
  PRIMARY_TENANT("Primary tenant"),
  GUARANTOR("Guarantor"),
  COSIGNER("Co-signer"),
  EXTRA_TENANT("Additional tenant");

  private final String displayName;
}
