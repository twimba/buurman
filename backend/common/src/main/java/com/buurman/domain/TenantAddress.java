package com.buurman.domain;

import java.time.Instant;
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
public class TenantAddress {

  public enum AddressType {
    CURRENT,
    MAILING,
    RELATIVE,
    WORK,
    HISTORIC
  }

  public enum AddressStatus {
    ACTIVE,
    INACTIVE
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID tenantId;
  private UUID teamId;
  private String street;
  private String city;
  private String postalCode;
  private String countryCode;
  private AddressType addressType;
  private AddressStatus status;
  @Builder.Default private Optional<Double> latitude = Optional.empty();
  @Builder.Default private Optional<Double> longitude = Optional.empty();
  @Builder.Default private Optional<String> geocodeAccuracy = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
