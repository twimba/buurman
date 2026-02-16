package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
  private String bankName;
  private String accountHolderName;
  private String iban;
  private String bicSwift;
  private String accountNumber;
  private String routingNumber;
  private String paymentReference;
  private String additionalDetails;
  private Boolean isDefault;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant deletedAt;
}
