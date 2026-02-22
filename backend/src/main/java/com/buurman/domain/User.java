package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class User {

  private UUID id;
  private @Nullable String identifier;
  private String keycloakId;
  private String email;
  private String firstName;
  private String lastName;
  private @Nullable UUID defaultTeamId;
  private @Nullable UUID activeTeamId;
  private @Nullable String phone;
  private @Nullable Instant emailVerifiedAt;
  private @Nullable Instant phoneVerifiedAt;
  private @Nullable Instant disabledAt;
  private Instant createdAt;
  private Instant updatedAt;

  public User(
      UUID id,
      String keycloakId,
      String email,
      String firstName,
      String lastName,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.keycloakId = keycloakId;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
