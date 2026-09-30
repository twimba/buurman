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
public class ContractTermination {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  private TerminationGivenBy givenBy;
  private LocalDate noticeDate;
  @Builder.Default private Optional<String> groundCode = Optional.empty();
  private LocalDate computedEndDate;
  private LocalDate effectiveEndDate;
  @Builder.Default private Optional<String> overrideReason = Optional.empty();
  @Builder.Default private Optional<LocalDate> inspectionDate = Optional.empty();
  @Builder.Default private Optional<UUID> noticeLetterDocumentId = Optional.empty();
  private ContractTerminationStatus status;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
