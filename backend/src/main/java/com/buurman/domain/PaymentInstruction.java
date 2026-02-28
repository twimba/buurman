package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "Supported payment method for rent collection")
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
  @Builder.Default private Optional<String> bankName = Optional.empty();
  @Builder.Default private Optional<String> accountHolderName = Optional.empty();
  @Builder.Default private Optional<String> iban = Optional.empty();
  @Builder.Default private Optional<String> bicSwift = Optional.empty();
  @Builder.Default private Optional<String> accountNumber = Optional.empty();
  @Builder.Default private Optional<String> routingNumber = Optional.empty();
  @Builder.Default private Optional<String> paymentReference = Optional.empty();
  @Builder.Default private Optional<String> additionalDetails = Optional.empty();
  @Builder.Default private Boolean isDefault = false;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
