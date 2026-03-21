package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.ContractExtension;

import jakarta.validation.constraints.Positive;
import com.buurman.util.Generated;

@Generated
public record CreateContractExtensionRequest(
    Optional<LocalDate> newEndDate,
    Optional<@Positive(message = "New rent amount must be positive") BigDecimal> newRentAmount,
    Optional<ContractExtension.RentAdjustmentType> rentAdjustmentType,
    Optional<BigDecimal> rentAdjustmentValue,
    Optional<String> notes) {}
