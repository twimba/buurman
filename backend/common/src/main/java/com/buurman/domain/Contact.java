package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
public class Contact {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private ContactType contactType;
  private String displayName;
  @Builder.Default private Optional<String> firstName = Optional.empty();
  @Builder.Default private Optional<String> lastName = Optional.empty();
  @Builder.Default private Optional<String> companyName = Optional.empty();
  @Builder.Default private Optional<String> tradeName = Optional.empty();
  @Builder.Default private Optional<String> industry = Optional.empty();
  @Builder.Default private Optional<String> email = Optional.empty();
  @Builder.Default private Optional<String> invoiceEmail = Optional.empty();
  @Builder.Default private Optional<String> phone = Optional.empty();
  @Builder.Default private Optional<String> website = Optional.empty();
  @Builder.Default private Optional<String> taxNumber = Optional.empty();
  @Builder.Default private Optional<String> idNumber = Optional.empty();
  @Builder.Default private Optional<LocalDate> dateOfBirth = Optional.empty();
  @Builder.Default private Optional<LocalDate> idExpiryDate = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private DataRetentionStatus dataRetentionStatus = DataRetentionStatus.ACTIVE;
  @Builder.Default private List<ContactTag> tags = List.of();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
