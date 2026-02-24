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
    isCustom = Objects.requireNonNullElse(isCustom, Optional.empty());
    customName = Objects.requireNonNullElse(customName, Optional.empty());
    customDescription = Objects.requireNonNullElse(customDescription, Optional.empty());
    customPaymentMethod = Objects.requireNonNullElse(customPaymentMethod, Optional.empty());
    customBankName = Objects.requireNonNullElse(customBankName, Optional.empty());
    customAccountHolderName = Objects.requireNonNullElse(customAccountHolderName, Optional.empty());
    customIban = Objects.requireNonNullElse(customIban, Optional.empty());
    customBicSwift = Objects.requireNonNullElse(customBicSwift, Optional.empty());
    customAccountNumber = Objects.requireNonNullElse(customAccountNumber, Optional.empty());
    customRoutingNumber = Objects.requireNonNullElse(customRoutingNumber, Optional.empty());
    customPaymentReference = Objects.requireNonNullElse(customPaymentReference, Optional.empty());
    customAdditionalDetails = Objects.requireNonNullElse(customAdditionalDetails, Optional.empty());
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
