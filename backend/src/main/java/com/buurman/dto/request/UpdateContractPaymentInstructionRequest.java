package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateContractPaymentInstructionRequest(
    Optional<String> paymentInstructionIdentifier,
    Optional<Boolean> isCustom,
    Optional<String> customName,
    Optional<String> customDescription,
    Optional<String> customPaymentMethod,
    Optional<String> customBankName,
    Optional<String> customAccountHolderName,
    @Size(max = 34) Optional<String> customIban,
    @Size(max = 11) Optional<String> customBicSwift,
    Optional<String> customAccountNumber,
    Optional<String> customRoutingNumber,
    Optional<String> customPaymentReference,
    Optional<String> customAdditionalDetails,
    @NotNull(message = "Effective from date is required") LocalDate effectiveFrom,
    Optional<String> notes) {

  public UpdateContractPaymentInstructionRequest {
    paymentInstructionIdentifier =
        Objects.requireNonNullElse(paymentInstructionIdentifier, Optional.empty());
  }
}
