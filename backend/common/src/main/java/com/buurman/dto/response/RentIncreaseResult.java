package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.identifier.ContractIdentifier;

public record RentIncreaseResult(
    ContractIdentifier contractIdentifier,
    String propertyName,
    boolean success,
    BigDecimal previousRentAmount,
    BigDecimal newRentAmount,
    LocalDate effectiveDate,
    int paymentsCancelled,
    int paymentsGenerated,
    Optional<String> error) {}
