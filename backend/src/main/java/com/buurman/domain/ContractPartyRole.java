package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "Role of a party within a rental contract")
@Getter
@RequiredArgsConstructor
public enum ContractPartyRole {
  PRIMARY_TENANT("Primary tenant"),
  GUARANTOR("Guarantor"),
  COSIGNER("Co-signer"),
  EXTRA_TENANT("Additional tenant");

  private final String displayName;
}
