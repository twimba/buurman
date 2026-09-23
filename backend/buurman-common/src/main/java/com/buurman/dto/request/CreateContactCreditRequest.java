package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreateContactCreditRequest(
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotBlank(message = "A reason is required") @Size(max = 1000) String reason,
    Optional<ContractIdentifier> contractIdentifier) {}
