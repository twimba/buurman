package com.buurman.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

  @Builder.Default
  private Optional<Map<String, List<String>>> policyMatrix =
      Optional.empty(); // country code -> allowed number types

  @Builder.Default private int maxCodesPerHour = 3;
  @Builder.Default private int verificationCodeExpiryMinutes = 10;
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();

  public boolean isAllowed(Optional<String> countryCode, Optional<String> numberType) {
    return policyMatrix
        .map(
            matrix -> {
              List<String> allowedTypes = countryCode.map(matrix::get).orElse(null);
              if (allowedTypes == null || allowedTypes.isEmpty()) {
                return false;
              }
              return numberType.map(allowedTypes::contains).orElse(true); // type unknown → allowed
            })
        .orElse(false);
  }
}
