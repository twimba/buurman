package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.PaymentInstruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentInstructionRequest(
    @NotBlank(message = "Name is required") String name,
    Optional<String> description,
    @NotNull(message = "Payment method is required") PaymentInstruction.PaymentMethod paymentMethod,
    Optional<String> bankName,
    Optional<String> accountHolderName,
    @Size(max = 34) Optional<String> iban,
    @Size(max = 11) Optional<String> bicSwift,
    Optional<String> accountNumber,
    Optional<String> routingNumber,
    Optional<String> paymentReference,
    Optional<String> additionalDetails,
    Optional<Boolean> isDefault) {
  public CreatePaymentInstructionRequest {
    description = Objects.requireNonNullElse(description, Optional.empty());
    bankName = Objects.requireNonNullElse(bankName, Optional.empty());
    accountHolderName = Objects.requireNonNullElse(accountHolderName, Optional.empty());
    iban = Objects.requireNonNullElse(iban, Optional.empty());
    bicSwift = Objects.requireNonNullElse(bicSwift, Optional.empty());
    accountNumber = Objects.requireNonNullElse(accountNumber, Optional.empty());
    routingNumber = Objects.requireNonNullElse(routingNumber, Optional.empty());
    paymentReference = Objects.requireNonNullElse(paymentReference, Optional.empty());
    additionalDetails = Objects.requireNonNullElse(additionalDetails, Optional.empty());
    isDefault = Objects.requireNonNullElse(isDefault, Optional.empty());
  }
}
