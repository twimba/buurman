package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.ContractExtension;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContractExtensionResponse(
    Sid identifier,
    Sid contractIdentifier,
    int extensionNumber,
    LocalDate previousEndDate,
    Optional<LocalDate> newEndDate,
    BigDecimal previousRentAmount,
    String previousRentCurrency,
    BigDecimal newRentAmount,
    String newRentCurrency,
    ContractExtension.RentAdjustmentType rentAdjustmentType,
    Optional<BigDecimal> rentAdjustmentValue,
    ContractExtension.ExtensionStatus status,
    ContractExtension.TriggerType triggerType,
    Optional<String> notes,
    Optional<String> declinedReason,
    Optional<Instant> activatedAt,
    Optional<Instant> confirmedAt,
    Optional<Instant> supersededAt,
    Instant createdAt) {}
