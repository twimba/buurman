package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
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
public class ContractPaymentInstruction {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID contractId;
  private @Nullable UUID paymentInstructionId;
  @Builder.Default private Boolean isCustom = false;
  private @Nullable String customName;
  private @Nullable String customDescription;
  private @Nullable String customPaymentMethod;
  private @Nullable String customBankName;
  private @Nullable String customAccountHolderName;
  private @Nullable String customIban;
  private @Nullable String customBicSwift;
  private @Nullable String customAccountNumber;
  private @Nullable String customRoutingNumber;
  private @Nullable String customPaymentReference;
  private @Nullable String customAdditionalDetails;
  private LocalDate effectiveFrom;
  private @Nullable LocalDate effectiveTo;
  private @Nullable String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
