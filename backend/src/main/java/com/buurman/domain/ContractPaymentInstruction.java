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
public class ContractPaymentInstruction {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  @Builder.Default private Optional<UUID> paymentInstructionId = Optional.empty();
  @Builder.Default private Boolean isCustom = false;
  @Builder.Default private Optional<String> customName = Optional.empty();
  @Builder.Default private Optional<String> customDescription = Optional.empty();
  @Builder.Default private Optional<String> customPaymentMethod = Optional.empty();
  @Builder.Default private Optional<String> customBankName = Optional.empty();
  @Builder.Default private Optional<String> customAccountHolderName = Optional.empty();
  @Builder.Default private Optional<String> customIban = Optional.empty();
  @Builder.Default private Optional<String> customBicSwift = Optional.empty();
  @Builder.Default private Optional<String> customAccountNumber = Optional.empty();
  @Builder.Default private Optional<String> customRoutingNumber = Optional.empty();
  @Builder.Default private Optional<String> customPaymentReference = Optional.empty();
  @Builder.Default private Optional<String> customAdditionalDetails = Optional.empty();
  private LocalDate effectiveFrom;
  @Builder.Default private Optional<LocalDate> effectiveTo = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
