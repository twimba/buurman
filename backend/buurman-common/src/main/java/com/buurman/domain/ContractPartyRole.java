package com.buurman.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractPartyRole {
  PRIMARY_TENANT("Primary tenant"),
  GUARANTOR("Guarantor"),
  COSIGNER("Co-signer"),
  EXTRA_TENANT("Additional tenant"),
  SIGNER("Signer"),
  CORPORATE_TENANT("Corporate tenant"),
  AUTHORIZED_REPRESENTATIVE("Authorized representative");

  private final String displayName;
}
