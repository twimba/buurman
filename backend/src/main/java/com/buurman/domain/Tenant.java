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
public class Tenant {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String firstName;
  private String lastName;
  private String email;
  private @Nullable String phone;
  private @Nullable String taxNumber;
  private @Nullable String idNumber;
  private @Nullable String additionalInfo;
  private @Nullable UUID currentPropertyId;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
