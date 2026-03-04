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
public class User {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String keycloakId;
  private String email;
  private String firstName;
  private String lastName;
  @Builder.Default private Optional<UUID> defaultTeamId = Optional.empty();
  @Builder.Default private Optional<UUID> activeTeamId = Optional.empty();
  @Builder.Default private Optional<String> phone = Optional.empty();
  @Builder.Default private Optional<Instant> emailVerifiedAt = Optional.empty();
  @Builder.Default private Optional<Instant> phoneVerifiedAt = Optional.empty();
  @Builder.Default private Optional<Instant> disabledAt = Optional.empty();
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

  public String getFullName() {
    return getFirstName() + " " + getLastName();
  }
}
