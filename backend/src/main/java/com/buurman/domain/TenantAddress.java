package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

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
  private Double latitude;
  private Double longitude;
  private String geocodeAccuracy;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;

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
      Double latitude,
      Double longitude,
      String geocodeAccuracy,
      Instant createdAt,
      Instant updatedAt,
      UUID createdBy,
      UUID updatedBy,
      Instant deletedAt) {
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
