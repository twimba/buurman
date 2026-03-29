package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
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
public class ContactNote {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contactId;
  private InteractionType interactionType;
  @Builder.Default private Optional<String> subject = Optional.empty();
  private String body;
  private Instant occurredAt;
  @Builder.Default private Optional<LocalDate> followUpDate = Optional.empty();
  private boolean followUpReminderSent;
  private boolean pinned;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
