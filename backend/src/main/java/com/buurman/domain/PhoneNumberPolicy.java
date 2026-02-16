package com.buurman.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PhoneNumberPolicy {

  private UUID id;
  private Map<String, List<String>> policyMatrix; // country code -> allowed number types
  private int maxCodesPerHour = 3;
  private int verificationCodeExpiryMinutes = 10;
  private Instant updatedAt;
  private String updatedBy;

  public boolean isAllowed(String countryCode, String numberType) {
    if (policyMatrix == null) {
      return false;
    }
    List<String> allowedTypes = policyMatrix.get(countryCode);
    if (allowedTypes == null || allowedTypes.isEmpty()) {
      return false;
    }
    if (numberType == null) {
      return true; // type unknown but country is allowed
    }
    return allowedTypes.contains(numberType);
  }
}
