package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
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
  private String identifier;
  private UUID tenantId;
  private UUID teamId;
  private String street;
  private String city;
  private String postalCode;
  private String country;
  private AddressType addressType;
  private AddressStatus status;
  private @Nullable Double latitude;
  private @Nullable Double longitude;
  private @Nullable String geocodeAccuracy;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;

  public TenantAddress(
      UUID id,
      UUID tenantId,
      UUID teamId,
      String street,
      String city,
      String postalCode,
      String country,
      AddressType addressType,
      AddressStatus status,
      @Nullable Double latitude,
      @Nullable Double longitude,
      @Nullable String geocodeAccuracy,
      Instant createdAt,
      Instant updatedAt,
      UUID createdBy,
      UUID updatedBy,
      @Nullable Instant deletedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.teamId = teamId;
    this.street = street;
    this.city = city;
    this.postalCode = postalCode;
    this.country = country;
    this.addressType = addressType;
    this.status = status;
    this.latitude = latitude;
    this.longitude = longitude;
    this.geocodeAccuracy = geocodeAccuracy;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
    this.deletedAt = deletedAt;
  }
}
