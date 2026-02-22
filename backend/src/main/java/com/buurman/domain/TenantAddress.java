package com.buurman.domain;

import java.time.Instant;
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
}
