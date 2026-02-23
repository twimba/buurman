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
public class PaymentInstruction {

  public enum PaymentMethod {
    BANK_TRANSFER,
    PAYPAL,
    CASH,
    CHECK,
    DIRECT_DEBIT,
    IDEAL_WERO,
    ZELLE,
    OTHER
  }

  private UUID id;
  private String identifier;
  private UUID teamId;
  private String name;
  private String description;
  private PaymentMethod paymentMethod;
  private @Nullable String bankName;
  private @Nullable String accountHolderName;
  private @Nullable String iban;
  private @Nullable String bicSwift;
  private @Nullable String accountNumber;
  private @Nullable String routingNumber;
  private @Nullable String paymentReference;
  private @Nullable String additionalDetails;
  @Builder.Default private Boolean isDefault = false;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
