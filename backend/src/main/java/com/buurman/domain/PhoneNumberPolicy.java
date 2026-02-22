package com.buurman.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberPolicy {

  private UUID id;
  private @Nullable Map<String, List<String>> policyMatrix; // country code -> allowed number types
  @Builder.Default private int maxCodesPerHour = 3;
  @Builder.Default private int verificationCodeExpiryMinutes = 10;
  private @Nullable Instant updatedAt;
  private @Nullable String updatedBy;

  public boolean isAllowed(@Nullable String countryCode, @Nullable String numberType) {
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
